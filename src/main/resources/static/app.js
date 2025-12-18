// ===== CLIQ24 DASHBOARD APPLICATION =====

class Cliq24Dashboard {
    constructor() {
        this.apiBaseUrl = window.location.origin;
        this.jwtToken = this.getJWTFromStorage();
        this.socialAccounts = [];
        this.currentUser = null;
        this.subscriptionStatus = null;
        this.allPlatforms = ['Facebook', 'Instagram', 'Twitter', 'LinkedIn', 'TikTok', 'YouTube', 'Snapchat'];
        this.confirmCallback = null;
        this.init();
    }

    // ===== INITIALIZATION =====
    async init() {
        this.setupEventListeners();
        this.addSVGGradient();

        // Check if we just logged out
        const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.get('logout') === 'true') {
            console.log('[INIT] Logout flag detected - showing login screen');
            this.clearJWTFromStorage();
            this.currentUser = null;
            this.showLoginPrompt();
            // Clean up URL
            window.history.replaceState({}, document.title, '/');
            return;
        }

        // Try to load user data - if authenticated via cookie, this will work
        // If not authenticated, the API will return 401 and we'll show login
        try {
            console.log('[INIT] Checking authentication via cookie...');
            await this.loadUserData();

            // If user data loaded successfully, user is authenticated
            if (this.currentUser) {
                await this.loadSocialAccounts();
                this.startAutoSync();
            } else {
                // Invalid token, clear and show login
                this.clearJWTFromStorage();
                this.showLoginPrompt();
            }
        } catch (error) {
            // Token invalid or expired, clear and show login
            console.error('Authentication error:', error);
            this.clearJWTFromStorage();
            this.showLoginPrompt();
        }
    }

    // ===== JWT TOKEN MANAGEMENT =====
    getJWTFromStorage() {
        try {
            return localStorage.getItem('cliq24_jwt') || sessionStorage.getItem('cliq24_jwt');
        } catch (e) {
            // iOS Safari may block localStorage, fallback to sessionStorage
            console.warn('localStorage blocked, using sessionStorage:', e);
            try {
                return sessionStorage.getItem('cliq24_jwt');
            } catch (e2) {
                console.error('Both localStorage and sessionStorage blocked:', e2);
                return null;
            }
        }
    }

    saveJWTToStorage(token) {
        try {
            localStorage.setItem('cliq24_jwt', token);
        } catch (e) {
            // iOS Safari may block localStorage, fallback to sessionStorage
            console.warn('localStorage blocked, using sessionStorage:', e);
            try {
                sessionStorage.setItem('cliq24_jwt', token);
            } catch (e2) {
                console.error('Both localStorage and sessionStorage blocked:', e2);
            }
        }
    }

    clearJWTFromStorage() {
        try {
            localStorage.removeItem('cliq24_jwt');
        } catch (e) {
            console.warn('localStorage access failed:', e);
        }
        try {
            sessionStorage.removeItem('cliq24_jwt');
        } catch (e) {
            console.warn('sessionStorage access failed:', e);
        }
    }

    // ===== API CALLS =====
    async apiCall(endpoint, options = {}) {
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        // Add Authorization header only if we have a token in localStorage (for email/password login)
        // For OAuth login, the token is in HttpOnly cookie and sent automatically
        if (this.jwtToken) {
            headers['Authorization'] = `Bearer ${this.jwtToken}`;
            console.log(`[API] Calling ${endpoint} with token from localStorage`);
        } else {
            console.log(`[API] Calling ${endpoint} (cookie auth)`);
        }

        try {
            console.log(`[API] Fetching: ${this.apiBaseUrl}${endpoint}`);
            const response = await fetch(`${this.apiBaseUrl}${endpoint}`, {
                ...options,
                headers
            });

            console.log(`[API] Response status: ${response.status} for ${endpoint}`);

            if (response.status === 401) {
                console.error(`[API] 401 Unauthorized for ${endpoint}`);
                // Only trigger handleUnauthorized for critical auth endpoints
                if (endpoint === '/auth/me') {
                    console.error('[API] Auth endpoint failed - logging out');
                    this.handleUnauthorized();
                } else {
                    console.warn(`[API] Non-critical endpoint ${endpoint} returned 401 - continuing`);
                }
                throw new Error('Unauthorized');
            }

            if (!response.ok) {
                console.error(`[API] Error ${response.status}: ${response.statusText}`);
                throw new Error(`API Error: ${response.statusText}`);
            }

            const data = await response.json();
            console.log(`[API] Success for ${endpoint}:`, data);
            return data;
        } catch (error) {
            console.error('[API] Call failed:', error);
            if (error.message === 'Unauthorized') {
                throw error; // Re-throw auth errors
            }
            this.showError('Failed to connect to server. Please try again.');
            throw error;
        }
    }

    async loadUserData() {
        const user = await this.apiCall('/auth/me');
        if (user) {
            this.currentUser = user;
            this.updateUserUI(user);
            await this.loadSubscriptionStatus();
        }
    }

    async loadSubscriptionStatus() {
        try {
            console.log('[DEBUG] Loading subscription status...');
            const status = await this.apiCall('/api/subscription/status');
            console.log('[DEBUG] Subscription status loaded:', status);
            if (status) {
                this.subscriptionStatus = status;
                this.updateSubscriptionUI();
            }
        } catch (error) {
            console.error('Failed to load subscription status:', error);
            // Don't throw - subscription is optional
            this.subscriptionStatus = { tier: 'FREE' }; // Default to free tier
        }
    }

    async loadSocialAccounts() {
        try {
            console.log('Loading social accounts...');
            const accounts = await this.apiCall('/api/social-accounts');
            console.log('Social accounts loaded:', accounts);

            if (accounts) {
                this.socialAccounts = accounts;
                console.log(`[DEBUG] Rendering ${accounts.length} social accounts`);
                this.renderSocialPods();
                this.updateOverallScore();
            } else {
                // Show empty state if no accounts
                this.socialAccounts = [];
                this.renderSocialPods();
                this.updateOverallScore();
            }
        } catch (error) {
            console.error('Failed to load social accounts:', error);
            // Don't throw - show empty state instead
            this.socialAccounts = [];
            this.renderSocialPods();
            this.updateOverallScore();
        }
    }

    async connectSocialAccount(platform) {
        try {
            // Facebook uses real OAuth flow - redirect to authorization
            if (platform === 'Facebook') {
                this.showInfo('Redirecting to Facebook...');
                this.closeModal();

                // Pass JWT token as query parameter if available (for email/password login)
                // For OAuth login, cookie authentication will be used automatically
                console.log('Connecting to Facebook, jwtToken:', this.jwtToken);
                if (this.jwtToken && typeof this.jwtToken === 'string') {
                    const token = this.jwtToken.replace('Bearer ', '');
                    console.log('Using JWT token for Facebook OAuth');
                    window.location.href = `${this.apiBaseUrl}/api/social-accounts/Facebook?token=${encodeURIComponent(token)}`;
                } else {
                    // Cookie-based auth, no token parameter needed
                    console.log('Using cookie-based auth for Facebook OAuth');
                    window.location.href = `${this.apiBaseUrl}/api/social-accounts/Facebook`;
                }
                return;
            }

            // Instagram uses real OAuth flow - redirect to authorization
            if (platform === 'Instagram') {
                this.showInfo('Redirecting to Instagram...');
                this.closeModal();

                // Pass JWT token as query parameter if available, otherwise use cookie auth
                if (this.jwtToken) {
                    const token = this.jwtToken.replace('Bearer ', '');
                    window.location.href = `${this.apiBaseUrl}/api/social-accounts/Instagram?token=${encodeURIComponent(token)}`;
                } else {
                    window.location.href = `${this.apiBaseUrl}/api/social-accounts/Instagram`;
                }
                return;
            }

            // LinkedIn uses real OAuth flow - redirect to authorization
            if (platform === 'LinkedIn') {
                this.showInfo('Redirecting to LinkedIn...');
                this.closeModal();

                // Pass JWT token as query parameter if available, otherwise use cookie auth
                if (this.jwtToken) {
                    const token = this.jwtToken.replace('Bearer ', '');
                    window.location.href = `${this.apiBaseUrl}/api/social-accounts/LinkedIn?token=${encodeURIComponent(token)}`;
                } else {
                    window.location.href = `${this.apiBaseUrl}/api/social-accounts/LinkedIn`;
                }
                return;
            }

            // Twitter uses real OAuth flow - redirect to authorization
            if (platform === 'Twitter') {
                this.showInfo('Redirecting to Twitter...');
                this.closeModal();

                // Pass JWT token as query parameter if available, otherwise use cookie auth
                if (this.jwtToken) {
                    const token = this.jwtToken.replace('Bearer ', '');
                    window.location.href = `${this.apiBaseUrl}/api/social-accounts/Twitter?token=${encodeURIComponent(token)}`;
                } else {
                    window.location.href = `${this.apiBaseUrl}/api/social-accounts/Twitter`;
                }
                return;
            }

            // TikTok uses real OAuth flow - redirect to authorization
            if (platform === 'TikTok') {
                this.showInfo('Redirecting to TikTok...');
                this.closeModal();

                // Pass JWT token as query parameter if available, otherwise use cookie auth
                if (this.jwtToken) {
                    const token = this.jwtToken.replace('Bearer ', '');
                    window.location.href = `${this.apiBaseUrl}/api/social-accounts/TikTok?token=${encodeURIComponent(token)}`;
                } else {
                    window.location.href = `${this.apiBaseUrl}/api/social-accounts/TikTok`;
                }
                return;
            }

            // YouTube and Snapchat use real OAuth flow - redirect to authorization
            if (platform === 'YouTube' || platform === 'Snapchat') {
                this.showInfo(`Redirecting to ${platform}...`);
                this.closeModal();

                // Pass JWT token as query parameter if available, otherwise use cookie auth
                if (this.jwtToken) {
                    const token = this.jwtToken.replace('Bearer ', '');
                    window.location.href = `${this.apiBaseUrl}/api/social-accounts/${platform}?token=${encodeURIComponent(token)}`;
                } else {
                    window.location.href = `${this.apiBaseUrl}/api/social-accounts/${platform}`;
                }
                return;
            }

            // Other platforms use demo mode for now
            this.showInfo(`Connecting to ${platform}...`);

            const response = await this.apiCall(`/api/social-accounts/${platform}`);

            if (response) {
                this.showSuccess(`${platform} connected successfully!`);
                await this.loadSocialAccounts();
                this.closeModal();
            }
        } catch (error) {
            console.error('Connection error:', error);
            this.showError(`Failed to connect ${platform}. Please try again.`);
        }
    }

    simulatePlatformConnection(platform) {
        const newAccount = {
            id: `demo-${Date.now()}`,
            platform: platform,
            username: `your${platform.toLowerCase()}`,
            metrics: {
                engagementScore: Math.floor(Math.random() * 40) + 60,
                connections: Math.floor(Math.random() * 50000) + 1000,
                posts: Math.floor(Math.random() * 500) + 50,
                pendingResponses: Math.floor(Math.random() * 50),
                newMessages: Math.floor(Math.random() * 100)
            }
        };

        this.socialAccounts.push(newAccount);
        this.renderSocialPods();
        this.updateOverallScore();
        this.closeModal();
        this.showSuccess(`${platform} connected successfully!`);
    }

    async syncAccount(accountId) {
        const response = await this.apiCall(`/api/social-accounts/${accountId}/sync`, {
            method: 'POST'
        });

        if (response) {
            this.showSuccess('Account synced successfully!');
            await this.loadSocialAccounts();
        } else {
            // Demo mode - simulate sync
            const account = this.socialAccounts.find(a => a.id === accountId);
            if (account) {
                account.metrics.engagementScore = Math.min(100, account.metrics.engagementScore + Math.floor(Math.random() * 5));
                this.renderSocialPods();
                this.updateOverallScore();
                this.showSuccess('Account synced successfully!');
            }
        }
    }

    async deleteAccount(accountId) {
        const response = await this.apiCall(`/api/social-accounts/${accountId}`, {
            method: 'DELETE'
        });

        if (response !== null) {
            this.showSuccess('Account disconnected successfully!');
            await this.loadSocialAccounts();
        } else {
            // Demo mode - remove from array
            this.socialAccounts = this.socialAccounts.filter(a => a.id !== accountId);
            this.renderSocialPods();
            this.updateOverallScore();
            this.showSuccess('Account disconnected successfully!');
        }
    }

    // ===== UI UPDATES =====
    updateUserUI(user) {
        const userName = document.getElementById('userName');
        const userAvatar = document.getElementById('userAvatar');

        if (userName) {
            userName.textContent = user.name || user.email;
        }

        if (userAvatar) {
            // Use 'picture' field from UserDTO
            if (user.picture) {
                // Clear existing background image first to force refresh
                userAvatar.style.backgroundImage = 'none';

                // Force browser to load new image by using setTimeout
                setTimeout(() => {
                    // For data URLs, use directly. For regular URLs, add cache buster
                    const pictureUrl = user.picture.startsWith('data:')
                        ? user.picture
                        : user.picture + '?t=' + Date.now();

                    userAvatar.style.backgroundImage = `url("${pictureUrl}")`;
                    userAvatar.style.backgroundSize = 'cover';
                    userAvatar.style.backgroundPosition = 'center';
                }, 10);
            }

            // Make avatar clickable to change picture (only add listener once)
            if (!userAvatar.dataset.listenerAdded) {
                userAvatar.style.cursor = 'pointer';
                userAvatar.title = 'Click to update your profile picture';
                userAvatar.addEventListener('click', () => this.showProfilePictureDialog());
                userAvatar.dataset.listenerAdded = 'true';
            }
        }
    }

    updateSubscriptionUI() {
        if (!this.subscriptionStatus) return;

        // Add or update subscription badge in header
        const userInfo = document.querySelector('.user-info');
        if (userInfo) {
            // Remove existing badge if any
            const existingBadge = userInfo.querySelector('.subscription-badge');
            if (existingBadge) existingBadge.remove();

            // Add tier badge
            const badge = document.createElement('span');
            badge.className = 'subscription-badge';
            badge.textContent = this.subscriptionStatus.tier;
            badge.style.cssText = `
                display: inline-block;
                padding: 2px 8px;
                margin-left: 8px;
                border-radius: 12px;
                font-size: 11px;
                font-weight: 600;
                ${this.subscriptionStatus.tier === 'PREMIUM' ?
                    'background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white;' :
                    'background: #e2e8f0; color: #64748b;'}
            `;
            const userName = document.getElementById('userName');
            if (userName) {
                userName.after(badge);
            }
        }

        // Add upgrade button if free tier
        if (this.subscriptionStatus.tier === 'FREE') {
            const header = document.querySelector('.header-content');
            if (header && !document.getElementById('upgradeBtn')) {
                const upgradeBtn = document.createElement('button');
                upgradeBtn.id = 'upgradeBtn';
                upgradeBtn.className = 'upgrade-btn';
                upgradeBtn.innerHTML = '⭐ Upgrade to Premium';
                upgradeBtn.style.cssText = `
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                    border: none;
                    padding: 10px 20px;
                    border-radius: 8px;
                    font-weight: 600;
                    cursor: pointer;
                    transition: transform 0.2s;
                    margin-left: auto;
                `;
                upgradeBtn.onmouseover = () => upgradeBtn.style.transform = 'scale(1.05)';
                upgradeBtn.onmouseout = () => upgradeBtn.style.transform = 'scale(1)';
                upgradeBtn.onclick = () => this.handleUpgrade();
                header.appendChild(upgradeBtn);
            }
        }
    }

    async handleUpgrade() {
        try {
            const response = await this.apiCall('/api/subscription/create-checkout-session', { method: 'POST' });
            if (response && response.url) {
                // Redirect to Stripe checkout
                window.location.href = response.url;
            }
        } catch (error) {
            console.error('Failed to create checkout session:', error);
            this.showError('Failed to start upgrade process. Please try again.');
        }
    }

    showProfilePictureDialog() {
        const currentPictureUrl = this.currentUser?.picture || '';

        // Create modal HTML
        const modalHtml = `
            <div class="modal-overlay" id="profilePictureModal">
                <div class="modal-container" style="max-width: 500px;">
                    <div class="modal-header">
                        <h2>Change Profile Picture</h2>
                        <button class="modal-close" id="profilePictureModalClose">✕</button>
                    </div>
                    <div class="modal-body" style="padding: 2rem;">
                        <div style="margin-bottom: 1.5rem; text-align: center;">
                            <div class="user-avatar" id="previewAvatar" style="width: 120px; height: 120px; margin: 0 auto; background-image: url(${currentPictureUrl}); background-size: cover; background-position: center; cursor: pointer; border: 3px solid rgba(255, 255, 255, 0.1);" title="Click to select file"></div>
                            <input type="file" id="fileInput" accept="image/jpeg,image/jpg,image/png,image/gif" style="display: none;" />
                        </div>
                        <div style="margin-bottom: 1rem;">
                            <button id="selectFileBtn" style="width: 100%; padding: 0.75rem; background: rgba(0, 212, 255, 0.15); border: 2px dashed rgba(0, 212, 255, 0.5); border-radius: 8px; color: #00d4ff; font-size: 0.95rem; cursor: pointer; transition: all 0.2s;">
                                📁 Choose Photo from Computer
                            </button>
                        </div>
                        <div id="selectedFileName" style="font-size: 0.85rem; color: var(--text-tertiary); text-align: center; margin-bottom: 1rem; min-height: 20px;"></div>
                        <div style="font-size: 0.85rem; color: var(--text-tertiary); margin-bottom: 1.5rem; text-align: center;">
                            Supported formats: JPG, PNG, GIF (max 5MB)
                        </div>
                        <div style="display: flex; gap: 1rem; justify-content: flex-end;">
                            <button id="profilePictureCancelBtn" class="modal-btn" style="background: rgba(255, 255, 255, 0.05); color: var(--text-secondary);">Cancel</button>
                            <button id="profilePictureSaveBtn" class="modal-btn" style="background: linear-gradient(135deg, var(--ambient-blue), var(--ambient-purple)); color: white;" disabled>Save</button>
                        </div>
                    </div>
                </div>
            </div>
        `;

        // Add modal to DOM
        const modalElement = document.createElement('div');
        modalElement.innerHTML = modalHtml;
        document.body.appendChild(modalElement.firstElementChild);

        // Get modal and elements
        const modal = document.getElementById('profilePictureModal');
        const closeBtn = document.getElementById('profilePictureModalClose');
        const cancelBtn = document.getElementById('profilePictureCancelBtn');
        const saveBtn = document.getElementById('profilePictureSaveBtn');
        const fileInput = document.getElementById('fileInput');
        const selectFileBtn = document.getElementById('selectFileBtn');
        const preview = document.getElementById('previewAvatar');
        const fileNameDisplay = document.getElementById('selectedFileName');

        let selectedFile = null;

        // Click preview or button to select file
        preview.addEventListener('click', () => fileInput.click());
        selectFileBtn.addEventListener('click', () => fileInput.click());

        // Handle file selection
        fileInput.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (!file) return;

            // Validate file size
            if (file.size > 5 * 1024 * 1024) {
                this.showError('File size must be less than 5MB');
                return;
            }

            // Validate file type
            const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif'];
            if (!validTypes.includes(file.type)) {
                this.showError('Please select a valid image file (JPG, PNG, or GIF)');
                return;
            }

            selectedFile = file;
            fileNameDisplay.textContent = '✓ ' + file.name;
            fileNameDisplay.style.color = '#00ffcc';
            saveBtn.disabled = false;
            saveBtn.style.opacity = '1';

            // Preview the image
            const reader = new FileReader();
            reader.onload = (e) => {
                preview.style.backgroundImage = `url(${e.target.result})`;
            };
            reader.readAsDataURL(file);
        });

        // Close modal function
        const closeModal = () => {
            modal.classList.remove('active');
            setTimeout(() => modal.remove(), 300);
        };

        // Event listeners
        closeBtn.addEventListener('click', closeModal);
        cancelBtn.addEventListener('click', closeModal);

        modal.addEventListener('click', (e) => {
            if (e.target === modal) closeModal();
        });

        saveBtn.addEventListener('click', async () => {
            if (!selectedFile) {
                this.showError('Please select a file');
                return;
            }

            // Show loading state
            saveBtn.disabled = true;
            saveBtn.textContent = 'Uploading...';

            try {
                // Create form data
                const formData = new FormData();
                formData.append('file', selectedFile);

                // Prepare headers - only add Authorization if we have a JWT token
                const headers = {};
                if (this.jwtToken && typeof this.jwtToken === 'string') {
                    headers['Authorization'] = `Bearer ${this.jwtToken}`;
                }

                // Upload file (credentials included for cookie-based auth)
                const response = await fetch(`${this.apiBaseUrl}/auth/me/picture/upload`, {
                    method: 'POST',
                    headers: headers,
                    credentials: 'include', // Important for cookie-based authentication
                    body: formData
                });

                if (!response.ok) {
                    const errorData = await response.json().catch(() => ({}));
                    throw new Error(errorData.message || 'Upload failed');
                }

                const updatedUser = await response.json();
                console.log('[Profile Upload] Updated user data:', {
                    userId: updatedUser.id,
                    pictureLength: updatedUser.picture ? updatedUser.picture.length : 0,
                    pictureType: updatedUser.picture ? (updatedUser.picture.startsWith('data:') ? 'data URL' : 'file URL') : 'none'
                });

                this.currentUser = updatedUser;
                this.updateUserUI(updatedUser);
                this.showSuccess('Profile picture updated successfully!');
                closeModal();
            } catch (error) {
                console.error('Upload error:', error);
                this.showError('Failed to upload profile picture. Please try again.');
                saveBtn.disabled = false;
                saveBtn.textContent = 'Save';
            }
        });

        // Show modal with animation
        setTimeout(() => modal.classList.add('active'), 10);
    }
    renderSocialPods() {
        const grid = document.getElementById('socialPodsGrid');
        if (!grid) return;

        grid.innerHTML = '';

        if (this.socialAccounts.length === 0) {
            // Show welcome message and platform selection grid
            grid.innerHTML = `
                <div style="grid-column: 1/-1; text-align: center; padding: 2rem 1rem; margin-bottom: 2rem;">
                    <div style="font-size: 3rem; margin-bottom: 1rem;">👋</div>
                    <h2 style="font-size: 1.8rem; margin-bottom: 0.5rem; background: linear-gradient(135deg, var(--ambient-blue), var(--ambient-teal)); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;">
                        Welcome to Cliq24!
                    </h2>
                    <p style="color: var(--text-secondary); font-size: 1.1rem; margin-bottom: 0.5rem;">
                        You're not tracking any social accounts yet
                    </p>
                    <p style="color: var(--text-tertiary); font-size: 0.95rem;">
                        Connect your first platform below to start tracking your social media performance
                    </p>
                </div>
            `;

            // Create platform selection grid
            const platformsContainer = document.createElement('div');
            platformsContainer.style.cssText = `
                grid-column: 1/-1;
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                gap: 1.5rem;
                padding: 1rem;
                max-width: 1000px;
                margin: 0 auto;
            `;

            this.allPlatforms.forEach(platform => {
                const platformCard = this.createEmptyStatePlatformCard(platform);
                platformsContainer.appendChild(platformCard);
            });

            grid.appendChild(platformsContainer);
            return;
        }

        this.socialAccounts.forEach(account => {
            try {
                const pod = this.createSocialPod(account);
                grid.appendChild(pod);
            } catch (error) {
                console.error('Failed to create pod for account:', account, error);
            }
        });
    }

    createEmptyStatePlatformCard(platform) {
        const card = document.createElement('div');
        card.className = `empty-state-platform-card platform-${platform.toLowerCase()}`;
        card.dataset.platform = platform;

        const platformIcon = this.getPlatformIcon(platform);
        const platformName = this.capitalizePlatform(platform);

        card.style.cssText = `
            background: var(--glass-bg);
            border: 2px solid var(--glass-border);
            border-radius: 1rem;
            padding: 2rem 1.5rem;
            text-align: center;
            cursor: pointer;
            transition: all 0.3s ease;
            position: relative;
            overflow: hidden;
        `;

        card.innerHTML = `
            <div style="font-size: 3rem; margin-bottom: 1rem;">${platformIcon}</div>
            <div style="font-size: 1.1rem; font-weight: 600; color: var(--text-primary); margin-bottom: 0.5rem;">
                ${platformName}
            </div>
            <div style="font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 1rem;">
                Track your ${platformName} performance
            </div>
            <div style="
                display: inline-flex;
                align-items: center;
                gap: 0.5rem;
                padding: 0.5rem 1rem;
                background: linear-gradient(135deg, var(--ambient-blue), var(--ambient-teal));
                border-radius: 0.5rem;
                color: white;
                font-size: 0.9rem;
                font-weight: 600;
            ">
                <span>+</span>
                <span>Connect</span>
            </div>
        `;

        // Hover effects
        card.addEventListener('mouseenter', () => {
            card.style.transform = 'translateY(-8px)';
            card.style.borderColor = 'var(--ambient-blue)';
            card.style.boxShadow = '0 10px 30px rgba(0, 212, 255, 0.3)';
        });

        card.addEventListener('mouseleave', () => {
            card.style.transform = 'translateY(0)';
            card.style.borderColor = 'var(--glass-border)';
            card.style.boxShadow = 'none';
        });

        // Click to connect
        card.addEventListener('click', () => {
            this.connectSocialAccount(platform);
        });

        return card;
    }

    createSocialPod(account) {
        const pod = document.createElement('div');
        pod.className = `social-pod pod-${account.platform.toLowerCase()}`;

        const platformIcon = this.getPlatformIcon(account.platform);

        const platformName = this.capitalizePlatform(account.platform);

        pod.innerHTML = `
            <div class="pod-header">
                <div class="pod-platform">
                    <div class="pod-icon">${platformIcon}</div>
                    <div>
                        <div class="pod-name">${platformName}</div>
                    </div>
                </div>
                <div class="pod-score">${account.metrics?.engagementScore || 0}</div>
            </div>
            <div class="pod-username">@${account.username || 'username'}</div>
            <div class="pod-stats">
                <div class="pod-stat">
                    <span class="pod-stat-label">Followers</span>
                    <span class="pod-stat-value">${this.formatNumber(account.metrics?.connections || 0)}</span>
                </div>
                <div class="pod-stat">
                    <span class="pod-stat-label">Posts</span>
                    <span class="pod-stat-value">${this.formatNumber(account.metrics?.posts || 0)}</span>
                </div>
                <div class="pod-stat">
                    <span class="pod-stat-label">Pending</span>
                    <span class="pod-stat-value">${this.formatNumber(account.metrics?.pendingResponses || 0)}</span>
                </div>
                <div class="pod-stat">
                    <span class="pod-stat-label">Messages</span>
                    <span class="pod-stat-value">${this.formatNumber(account.metrics?.newMessages || 0)}</span>
                </div>
            </div>
            <div class="pod-actions">
                ${account.platform?.toLowerCase() === 'facebook' ? `
                <button class="pod-action-btn manage" data-id="${account.id}">
                    <span>📊</span>
                    <span>Manage</span>
                </button>
                ` : ''}
                <button class="pod-action-btn sync" data-id="${account.id}">
                    <span>🔄</span>
                    <span>Sync</span>
                </button>
                <button class="pod-action-btn disconnect" data-id="${account.id}">
                    <span>✕</span>
                    <span>Disconnect</span>
                </button>
            </div>
        `;

        // Add event listeners for action buttons
        const manageBtn = pod.querySelector('.pod-action-btn.manage');
        const syncBtn = pod.querySelector('.pod-action-btn.sync');
        const disconnectBtn = pod.querySelector('.pod-action-btn.disconnect');

        // Manage button (Facebook only)
        if (manageBtn) {
            manageBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                window.location.href = `/facebook-dashboard.html?id=${account.id}`;
            });
        }

        syncBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            this.syncAccount(account.id);
        });

        disconnectBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            this.confirmAction(
                'Disconnect Account?',
                `Are you sure you want to disconnect your ${account.platform} account (@${account.username})? This action cannot be undone.`,
                () => this.deleteAccount(account.id)
            );
        });

        return pod;
    }

    updateOverallScore() {
        if (this.socialAccounts.length === 0) {
            this.setOverallScore(0, 'No Accounts Connected');
            return;
        }

        // Calculate average score
        const totalScore = this.socialAccounts.reduce((sum, account) => {
            return sum + (account.metrics?.engagementScore || 0);
        }, 0);

        const avgScore = Math.round(totalScore / this.socialAccounts.length);

        // Calculate totals
        const totalFollowers = this.socialAccounts.reduce((sum, account) => {
            return sum + (account.metrics?.connections || 0);
        }, 0);

        const totalPosts = this.socialAccounts.reduce((sum, account) => {
            return sum + (account.metrics?.posts || 0);
        }, 0);

        // Update UI
        this.setOverallScore(avgScore, this.getScoreLabel(avgScore));

        document.getElementById('totalAccounts').textContent = this.socialAccounts.length;
        document.getElementById('totalFollowers').textContent = this.formatNumber(totalFollowers);
        document.getElementById('totalPosts').textContent = this.formatNumber(totalPosts);
    }

    setOverallScore(score, label) {
        const scoreValue = document.getElementById('overallScore');
        const scoreLabel = document.getElementById('scoreLabel');
        const scoreCircle = document.getElementById('scoreCircle');

        if (scoreValue) {
            scoreValue.textContent = score;
        }

        if (scoreLabel) {
            scoreLabel.textContent = label;
        }

        if (scoreCircle) {
            // Animate the circle (534 is the circumference for radius 85)
            const circumference = 534;
            const offset = circumference - (score / 100) * circumference;
            scoreCircle.style.strokeDashoffset = offset;
        }
    }

    getScoreLabel(score) {
        if (score >= 80) return 'Crushing It! 🔥';
        if (score >= 60) return 'Doing Well 👍';
        if (score >= 40) return 'Needs Attention ⚠️';
        if (score >= 1) return 'Falling Behind 📉';
        return 'Getting Started';
    }

    getPlatformIcon(platform) {
        const icons = {
            'facebook': 'f',
            'instagram': '📷',
            'twitter': '🐦',
            'linkedin': 'in',
            'tiktok': '🎵',
            'youtube': '▶',
            'snapchat': '👻'
        };
        return icons[platform.toLowerCase()] || '📱';
    }

    capitalizePlatform(platform) {
        const names = {
            'facebook': 'Facebook',
            'instagram': 'Instagram',
            'twitter': 'Twitter',
            'linkedin': 'LinkedIn',
            'tiktok': 'TikTok',
            'youtube': 'YouTube',
            'snapchat': 'Snapchat'
        };
        return names[platform.toLowerCase()] || platform;
    }

    formatNumber(num) {
        if (num >= 1000000) {
            return (num / 1000000).toFixed(1) + 'M';
        }
        if (num >= 1000) {
            return (num / 1000).toFixed(1) + 'K';
        }
        return num.toString();
    }

    // ===== MODAL MANAGEMENT =====
    populateModal() {
        const connectedPlatforms = document.getElementById('connectedPlatforms');
        const availablePlatforms = document.getElementById('availablePlatforms');
        const connectedSection = document.getElementById('connectedSection');

        // Get connected platform names
        const connectedNames = this.socialAccounts.map(acc => acc.platform);

        // Populate connected platforms
        if (connectedNames.length > 0) {
            connectedSection.style.display = 'block';
            connectedPlatforms.innerHTML = '';

            this.socialAccounts.forEach(account => {
                const item = this.createConnectedPlatformItem(account);
                connectedPlatforms.appendChild(item);
            });
        } else {
            connectedSection.style.display = 'none';
        }

        // Populate available platforms
        availablePlatforms.innerHTML = '';
        this.allPlatforms.forEach(platform => {
            const isConnected = connectedNames.includes(platform);
            const btn = this.createPlatformButton(platform, isConnected);
            availablePlatforms.appendChild(btn);
        });
    }

    createConnectedPlatformItem(account) {
        const div = document.createElement('div');
        div.className = `connected-platform-item platform-${account.platform.toLowerCase()}`;

        const platformIcon = this.getPlatformIcon(account.platform);
        const platformName = this.capitalizePlatform(account.platform);

        div.innerHTML = `
            <div class="platform-item-left">
                <div class="platform-item-icon">${platformIcon}</div>
                <div class="platform-item-info">
                    <div class="platform-item-name">${platformName}</div>
                    <div class="platform-item-username">@${account.username || 'username'}</div>
                    <div class="platform-item-status">
                        <span class="status-dot"></span>
                        <span>Connected</span>
                    </div>
                </div>
            </div>
            <div class="platform-item-actions">
                <button class="icon-btn sync-btn" data-id="${account.id}" title="Sync">🔄</button>
                <button class="icon-btn disconnect-btn" data-id="${account.id}" title="Disconnect">✕</button>
            </div>
        `;

        // Add event listeners
        const syncBtn = div.querySelector('.sync-btn');
        const disconnectBtn = div.querySelector('.disconnect-btn');

        syncBtn.addEventListener('click', () => {
            this.syncAccount(account.id);
        });

        disconnectBtn.addEventListener('click', () => {
            this.confirmAction(
                'Disconnect Account?',
                `Are you sure you want to disconnect your ${account.platform} account (@${account.username})?`,
                () => {
                    this.deleteAccount(account.id);
                    this.closeModal();
                }
            );
        });

        return div;
    }

    createPlatformButton(platform, isConnected) {
        const button = document.createElement('button');
        button.className = `platform-btn ${isConnected ? 'connected' : 'available'} platform-${platform.toLowerCase()}`;
        button.dataset.platform = platform.toLowerCase();

        const platformIcon = this.getPlatformIcon(platform);

        button.innerHTML = `
            <div class="platform-icon">${platformIcon}</div>
            <span>${platform}</span>
            ${isConnected ? '<div class="platform-status-badge">✓</div>' : ''}
        `;

        if (!isConnected) {
            button.addEventListener('click', () => {
                this.handlePlatformConnect(platform);
            });
        }

        return button;
    }

    handlePlatformConnect(platform) {
        this.closeModal();
        this.connectSocialAccount(platform);
    }

    // ===== CONFIRMATION MODAL =====
    confirmAction(title, message, callback) {
        const modal = document.getElementById('confirmModal');
        const titleEl = document.getElementById('confirmTitle');
        const messageEl = document.getElementById('confirmMessage');

        titleEl.textContent = title;
        messageEl.textContent = message;

        this.confirmCallback = callback;
        modal.classList.add('active');
    }

    closeConfirmModal() {
        const modal = document.getElementById('confirmModal');
        modal.classList.remove('active');
        this.confirmCallback = null;
    }

    executeConfirm() {
        if (this.confirmCallback) {
            this.confirmCallback();
        }
        this.closeConfirmModal();
    }

    // ===== DEMO DATA =====
    loadDemoData() {
        this.socialAccounts = [
            {
                id: 'demo-1',
                platform: 'Facebook',
                username: 'yourpage',
                metrics: {
                    engagementScore: 85,
                    connections: 12500,
                    posts: 342,
                    pendingResponses: 12,
                    newMessages: 24
                }
            },
            {
                id: 'demo-2',
                platform: 'Instagram',
                username: 'yourhandle',
                metrics: {
                    engagementScore: 92,
                    connections: 28300,
                    posts: 567,
                    pendingResponses: 8,
                    newMessages: 45
                }
            },
            {
                id: 'demo-3',
                platform: 'Twitter',
                username: 'yourtwitter',
                metrics: {
                    engagementScore: 78,
                    connections: 8900,
                    posts: 1243,
                    pendingResponses: 34,
                    newMessages: 67
                }
            },
            {
                id: 'demo-4',
                platform: 'LinkedIn',
                username: 'yourprofile',
                metrics: {
                    engagementScore: 88,
                    connections: 3450,
                    posts: 156,
                    pendingResponses: 5,
                    newMessages: 18
                }
            }
        ];

        this.currentUser = {
            name: 'Demo User',
            email: 'demo@cliq24.app'
        };

        this.updateUserUI(this.currentUser);
        this.renderSocialPods();
        this.updateOverallScore();
    }

    // ===== EVENT LISTENERS =====
    setupEventListeners() {
        // Logout button
        const logoutBtn = document.getElementById('logoutBtn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                console.log('[Event] Logout button clicked');
                this.logout();
            });
        }

        // Add account button
        const addBtn = document.getElementById('addAccountBtn');
        if (addBtn) {
            addBtn.addEventListener('click', () => this.openModal());
        }

        // Modal close button
        const closeBtn = document.getElementById('modalClose');
        if (closeBtn) {
            closeBtn.addEventListener('click', () => this.closeModal());
        }

        // Modal background click
        const modal = document.getElementById('addAccountModal');
        if (modal) {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    this.closeModal();
                }
            });
        }

        // Confirm modal buttons
        const confirmClose = document.getElementById('confirmModalClose');
        const confirmCancel = document.getElementById('confirmCancel');
        const confirmOk = document.getElementById('confirmOk');
        const confirmModal = document.getElementById('confirmModal');

        if (confirmClose) {
            confirmClose.addEventListener('click', () => this.closeConfirmModal());
        }

        if (confirmCancel) {
            confirmCancel.addEventListener('click', () => this.closeConfirmModal());
        }

        if (confirmOk) {
            confirmOk.addEventListener('click', () => this.executeConfirm());
        }

        if (confirmModal) {
            confirmModal.addEventListener('click', (e) => {
                if (e.target === confirmModal) {
                    this.closeConfirmModal();
                }
            });
        }
    }

    openModal() {
        this.populateModal();
        const modal = document.getElementById('addAccountModal');
        if (modal) {
            modal.classList.add('active');
        }
    }

    closeModal() {
        const modal = document.getElementById('addAccountModal');
        if (modal) {
            modal.classList.remove('active');
        }
    }

    // ===== AUTO-SYNC =====
    startAutoSync() {
        // Sync every 5 minutes
        setInterval(() => {
            if (this.jwtToken) {
                this.loadSocialAccounts();
            }
        }, 5 * 60 * 1000);
    }

    // ===== NOTIFICATIONS =====
    showSuccess(message) {
        this.showNotification(message, 'success');
    }

    showError(message) {
        this.showNotification(message, 'error');
    }

    showInfo(message) {
        this.showNotification(message, 'info');
    }

    showAccountLimitError() {
        const modal = document.createElement('div');
        modal.className = 'modal active';
        modal.innerHTML = `
            <div class="modal-content" style="max-width: 500px; text-align: center;">
                <h2 style="margin-bottom: 16px;">🔒 Account Limit Reached</h2>
                <p style="color: #64748b; margin-bottom: 24px;">
                    You've reached the limit of 2 social media accounts on the Free plan.
                    <br><br>
                    <strong>Upgrade to Premium to connect unlimited accounts!</strong>
                </p>
                <div style="background: #f8fafc; padding: 16px; border-radius: 8px; margin-bottom: 24px;">
                    <div style="font-size: 24px; font-weight: 700; color: #1e293b; margin-bottom: 4px;">
                        ${this.subscriptionStatus?.tier === 'FREE' ? 'Unlock Premium' : 'Premium Plan'}
                    </div>
                    <div style="color: #64748b;">Unlimited social accounts</div>
                </div>
                <div style="display: flex; gap: 12px; justify-content: center;">
                    <button id="upgradeNowBtn" style="
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        border: none;
                        padding: 12px 32px;
                        border-radius: 8px;
                        font-weight: 600;
                        cursor: pointer;
                        flex: 1;
                        max-width: 200px;
                    ">⭐ Upgrade Now</button>
                    <button id="closeLimitModal" style="
                        background: #e2e8f0;
                        color: #64748b;
                        border: none;
                        padding: 12px 32px;
                        border-radius: 8px;
                        font-weight: 600;
                        cursor: pointer;
                        flex: 1;
                        max-width: 200px;
                    ">Maybe Later</button>
                </div>
            </div>
        `;

        document.body.appendChild(modal);

        // Helper function to safely remove modal
        const removeModal = () => {
            if (modal && modal.parentNode === document.body) {
                document.body.removeChild(modal);
            }
        };

        document.getElementById('upgradeNowBtn').addEventListener('click', () => {
            removeModal();
            this.handleUpgrade();
        });

        document.getElementById('closeLimitModal').addEventListener('click', () => {
            removeModal();
        });

        // Close on backdrop click
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                removeModal();
            }
        });
    }

    showNotification(message, type = 'info') {
        console.log(`[${type.toUpperCase()}] ${message}`);

        // Create toast
        const toast = document.createElement('div');
        toast.style.cssText = `
            position: fixed;
            top: 100px;
            right: 20px;
            background: ${type === 'error' ? '#ff006e' : type === 'success' ? '#00ffcc' : '#00d4ff'};
            color: #0a0a0f;
            padding: 1rem 1.5rem;
            border-radius: 12px;
            font-weight: 600;
            z-index: 10000;
            animation: slideIn 0.3s ease;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
        `;
        toast.textContent = message;
        document.body.appendChild(toast);

        setTimeout(() => {
            toast.style.animation = 'slideOut 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    }

    showLoginPrompt() {
        const loginUrl = `${this.apiBaseUrl}/auth/google`;

        // Create global function for button click
        window.handleGoogleLogin = function() {
            window.location.href = loginUrl;
        };

        // Hide the main dashboard
        const dashboard = document.querySelector('.dashboard');
        if (dashboard) {
            dashboard.style.display = 'none';
        }

        // Hide user info in header
        const userInfo = document.querySelector('.user-info');
        if (userInfo) {
            userInfo.style.display = 'none';
        }

        // Create login screen with scrollable content
        const loginScreen = document.createElement('div');
        loginScreen.style.cssText = 'position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: var(--bg-primary); z-index: 9999; overflow-y: auto; display: block;';
        loginScreen.innerHTML = `
            <div style="min-height: 100vh; display: flex; flex-direction: column; padding: 2rem 0;">
                <div class="login-container" style="max-width: 500px; width: 90%; margin: 0 auto; flex-shrink: 0;">
                    <div class="login-content">
                        <img src="logo.PNG" alt="Cliq24 Logo" class="login-logo" />
                        <h1 class="login-title">Welcome to CLIQ24</h1>
                        <p class="login-subtitle">Your Social Media Command Center</p>
                        <p class="login-description">Connect and manage all your social media accounts in one place. Track engagement, analyze performance, and stay on top of your digital presence.</p>
                        <button class="login-btn" id="loginBtn" onclick="window.handleGoogleLogin()" style="cursor: pointer; pointer-events: auto;">
                            <svg class="google-icon" viewBox="0 0 24 24" width="20" height="20">
                                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                            </svg>
                            <span>Sign in with Google</span>
                        </button>
                        <div style="margin: 1.5rem 0; text-align: center; color: var(--text-tertiary); font-size: 0.9rem;">
                            — or —
                        </div>
                        <a href="/register.html" class="register-link" style="display: block; text-align: center; color: var(--ambient-blue); text-decoration: none; font-size: 1rem; padding: 0.75rem; border: 1px solid var(--glass-border); border-radius: 0.5rem; transition: all 0.3s ease;">
                            Create an account with email
                        </a>
                        <div style="margin-top: 1rem; text-align: center;">
                            <span style="color: var(--text-tertiary); font-size: 0.85rem;">Already have an account? </span>
                            <a href="/login.html" style="color: var(--ambient-blue); text-decoration: none; font-size: 0.85rem;">Sign in</a>
                        </div>
                        <div class="login-features">
                            <a href="/track-performance.html" class="feature-item" style="text-decoration: none; color: inherit;">
                                <span class="feature-icon">📊</span>
                                <span>Track Performance</span>
                            </a>
                            <a href="/connect-platforms.html" class="feature-item" style="text-decoration: none; color: inherit;">
                                <span class="feature-icon">🔗</span>
                                <span>Connect Platforms</span>
                            </a>
                            <a href="/analyze-metrics.html" class="feature-item" style="text-decoration: none; color: inherit;">
                                <span class="feature-icon">📈</span>
                                <span>Analyze Metrics</span>
                            </a>
                        </div>
                    </div>
                </div>

                <footer class="footer" style="width: 100%; margin-top: auto; padding: 3rem 0 2rem 0; flex-shrink: 0;">
                    <div class="container" style="max-width: 1200px; margin: 0 auto; padding: 0 2rem;">
                        <div class="footer-content" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 2rem;">
                            <div class="footer-section">
                                <h4 style="color: var(--ambient-blue); margin-bottom: 1rem; font-size: 1rem; font-weight: 600;">Cliq24</h4>
                                <p style="color: var(--text-tertiary); font-size: 0.875rem; opacity: 0.7;">&copy; 2025 Cliq24. All rights reserved.</p>
                            </div>
                            <div class="footer-section">
                                <h4 style="color: var(--ambient-blue); margin-bottom: 1rem; font-size: 1rem; font-weight: 600;">Product</h4>
                                <div class="footer-links" style="display: flex; flex-direction: column; gap: 0.5rem;">
                                    <a href="/index.html" style="color: var(--text-secondary); text-decoration: none; font-size: 0.875rem; transition: color 0.2s;">Dashboard</a>
                                    <a href="/subscribe.html" style="color: var(--text-secondary); text-decoration: none; font-size: 0.875rem; transition: color 0.2s;">Subscribe</a>
                                    <a href="/connect-platforms.html" style="color: var(--text-secondary); text-decoration: none; font-size: 0.875rem; transition: color 0.2s;">Connect Platforms</a>
                                    <a href="/track-performance.html" style="color: var(--text-secondary); text-decoration: none; font-size: 0.875rem; transition: color 0.2s;">Track Performance</a>
                                    <a href="/analyze-metrics.html" style="color: var(--text-secondary); text-decoration: none; font-size: 0.875rem; transition: color 0.2s;">Analyze Metrics</a>
                                </div>
                            </div>
                            <div class="footer-section">
                                <h4 style="color: var(--ambient-blue); margin-bottom: 1rem; font-size: 1rem; font-weight: 600;">Account</h4>
                                <div class="footer-links" style="display: flex; flex-direction: column; gap: 0.5rem;">
                                    <a href="/login.html" style="color: var(--text-secondary); text-decoration: none; font-size: 0.875rem; transition: color 0.2s;">Login</a>
                                    <a href="/register.html" style="color: var(--text-secondary); text-decoration: none; font-size: 0.875rem; transition: color 0.2s;">Register</a>
                                </div>
                            </div>
                            <div class="footer-section">
                                <h4 style="color: var(--ambient-blue); margin-bottom: 1rem; font-size: 1rem; font-weight: 600;">Legal</h4>
                                <div class="footer-links" style="display: flex; flex-direction: column; gap: 0.5rem;">
                                    <a href="/privacy-policy.html" style="color: var(--text-secondary); text-decoration: none; font-size: 0.875rem; transition: color 0.2s;">Privacy Policy</a>
                                    <a href="/terms-of-service.html" style="color: var(--text-secondary); text-decoration: none; font-size: 0.875rem; transition: color 0.2s;">Terms of Service</a>
                                    <a href="/refund-policy.html" style="color: var(--text-secondary); text-decoration: none; font-size: 0.875rem; transition: color 0.2s;">Refund Policy</a>
                                    <a href="/data-deletion.html" style="color: var(--text-secondary); text-decoration: none; font-size: 0.875rem; transition: color 0.2s;">Delete My Data</a>
                                </div>
                            </div>
                        </div>
                    </div>
                </footer>
            </div>
        `;

        document.body.appendChild(loginScreen);

        // Add login button event listener
        const loginBtn = document.getElementById('loginBtn');
        if (loginBtn) {
            loginBtn.addEventListener('click', () => {
                window.location.href = loginUrl;
            });
        }
    }

    handleUnauthorized() {
        this.clearJWTFromStorage();
        this.currentUser = null;
        this.showLoginPrompt();
    }

    logout() {
        try {
            console.log('[Logout] Starting logout process...');

            // Clear localStorage/sessionStorage JWT immediately
            try {
                this.clearJWTFromStorage();
            } catch (e) {
                console.error('[Logout] Error clearing JWT (continuing):', e);
            }

            // Clear current user
            this.currentUser = null;

            console.log('[Logout] Cleared local storage and current user');

            // Call backend to clear cookie (fire and forget - don't wait)
            try {
                fetch(`${this.apiBaseUrl}/auth/logout`, {
                    method: 'POST',
                    credentials: 'include'
                }).then(response => {
                    console.log('[Logout] Backend response:', response.status);
                }).catch(error => {
                    console.error('[Logout] Backend error (ignoring):', error);
                });
            } catch (e) {
                console.error('[Logout] Fetch error (ignoring):', e);
            }

        } catch (error) {
            console.error('[Logout] Unexpected error (will still redirect):', error);
        } finally {
            // ALWAYS redirect, no matter what errors occurred above
            console.log('[Logout] Redirecting NOW (forced)...');
            try {
                window.location.replace('/?logout=true');
            } catch (e) {
                // If replace fails, try href as backup
                console.error('[Logout] Replace failed, using href:', e);
                window.location.href = '/?logout=true';
            }
        }
    }

    // ===== ADD SVG GRADIENT =====
    addSVGGradient() {
        const heroScore = document.querySelector('.hero-score-container');
        if (!heroScore) return;

        // Gradient is now in HTML, but keep this for compatibility
    }
}

// ===== ANIMATION STYLES =====
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(400px);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }

    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(400px);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);

// ===== INITIALIZE APP =====
document.addEventListener('DOMContentLoaded', () => {
    window.cliq24App = new Cliq24Dashboard();
});

// ===== HANDLE OAUTH CALLBACK =====
const urlParams = new URLSearchParams(window.location.search);
const token = urlParams.get('token');
if (token) {
    localStorage.setItem('cliq24_jwt', token);
    window.history.replaceState({}, document.title, window.location.pathname);
    window.location.reload();
}

// Handle Facebook connection callback
const facebookConnected = urlParams.get('facebook_connected');
const facebookError = urlParams.get('facebook_error');

if (facebookConnected === 'true') {
    setTimeout(() => {
        if (window.cliq24App) {
            window.cliq24App.showSuccess('Facebook connected successfully!');
            window.cliq24App.loadSocialAccounts();
        }
    }, 1000);
    window.history.replaceState({}, document.title, window.location.pathname);
}

if (facebookError) {
    setTimeout(() => {
        if (window.cliq24App) {
            const errorMsg = decodeURIComponent(facebookError);
            if (errorMsg.includes('Account limit reached')) {
                window.cliq24App.showAccountLimitError();
            } else {
                window.cliq24App.showError('Facebook connection failed: ' + errorMsg);
            }
        }
    }, 1000);
    window.history.replaceState({}, document.title, window.location.pathname);
}

// Handle Instagram connection callback
const instagramConnected = urlParams.get('instagram_connected');
const instagramError = urlParams.get('instagram_error');

if (instagramConnected === 'true') {
    setTimeout(() => {
        if (window.cliq24App) {
            window.cliq24App.showSuccess('Instagram connected successfully!');
            window.cliq24App.loadSocialAccounts();
        }
    }, 1000);
    window.history.replaceState({}, document.title, window.location.pathname);
}

if (instagramError) {
    setTimeout(() => {
        if (window.cliq24App) {
            window.cliq24App.showError('Instagram connection failed: ' + decodeURIComponent(instagramError));
        }
    }, 1000);
    window.history.replaceState({}, document.title, window.location.pathname);
}

// Handle LinkedIn connection callback
const linkedinConnected = urlParams.get('linkedin_connected');
const linkedinError = urlParams.get('linkedin_error');

if (linkedinConnected === 'true') {
    setTimeout(() => {
        if (window.cliq24App) {
            window.cliq24App.showSuccess('LinkedIn connected successfully!');
            window.cliq24App.loadSocialAccounts();
        }
    }, 1000);
    window.history.replaceState({}, document.title, window.location.pathname);
}

if (linkedinError) {
    setTimeout(() => {
        if (window.cliq24App) {
            window.cliq24App.showError('LinkedIn connection failed: ' + decodeURIComponent(linkedinError));
        }
    }, 1000);
    window.history.replaceState({}, document.title, window.location.pathname);
}

// Handle Snapchat connection callback
const snapchatConnected = urlParams.get('snapchat_connected');
const snapchatError = urlParams.get('snapchat_error');

if (snapchatConnected === 'true') {
    setTimeout(() => {
        if (window.cliq24App) {
            window.cliq24App.showSuccess('Snapchat connected successfully!');
            window.cliq24App.loadSocialAccounts();
        }
    }, 1000);
    window.history.replaceState({}, document.title, window.location.pathname);
}

if (snapchatError) {
    setTimeout(() => {
        if (window.cliq24App) {
            window.cliq24App.showError('Snapchat connection failed: ' + decodeURIComponent(snapchatError));
        }
    }, 1000);
    window.history.replaceState({}, document.title, window.location.pathname);
}

// Handle Twitter connection callback
const twitterConnected = urlParams.get('twitter_connected');
const twitterError = urlParams.get('twitter_error');

if (twitterConnected === 'true') {
    setTimeout(() => {
        if (window.cliq24App) {
            window.cliq24App.showSuccess('Twitter connected successfully!');
            window.cliq24App.loadSocialAccounts();
        }
    }, 1000);
    window.history.replaceState({}, document.title, window.location.pathname);
}

if (twitterError) {
    setTimeout(() => {
        if (window.cliq24App) {
            const errorMsg = decodeURIComponent(twitterError);
            if (errorMsg.includes('Account limit reached')) {
                window.cliq24App.showAccountLimitError();
            } else {
                window.cliq24App.showError('Twitter connection failed: ' + errorMsg);
            }
        }
    }, 1000);
    window.history.replaceState({}, document.title, window.location.pathname);
}

// Handle YouTube connection callback
const youtubeConnected = urlParams.get('youtube_connected');
const youtubeError = urlParams.get('youtube_error');

if (youtubeConnected === 'true') {
    setTimeout(() => {
        if (window.cliq24App) {
            window.cliq24App.showSuccess('YouTube connected successfully!');
            window.cliq24App.loadSocialAccounts();
        }
    }, 1000);
    window.history.replaceState({}, document.title, window.location.pathname);
}

if (youtubeError) {
    setTimeout(() => {
        if (window.cliq24App) {
            const errorMsg = decodeURIComponent(youtubeError);
            if (errorMsg.includes('Account limit reached')) {
                window.cliq24App.showAccountLimitError();
            } else {
                window.cliq24App.showError('YouTube connection failed: ' + errorMsg);
            }
        }
    }, 1000);
    window.history.replaceState({}, document.title, window.location.pathname);
}

// Handle TikTok connection callback
const tiktokConnected = urlParams.get('tiktok_connected');
const tiktokError = urlParams.get('tiktok_error');

if (tiktokConnected === 'true') {
    setTimeout(() => {
        if (window.cliq24App) {
            window.cliq24App.showSuccess('TikTok connected successfully!');
            window.cliq24App.loadSocialAccounts();
        }
    }, 1000);
    window.history.replaceState({}, document.title, window.location.pathname);
}

if (tiktokError) {
    setTimeout(() => {
        if (window.cliq24App) {
            const errorMsg = decodeURIComponent(tiktokError);
            if (errorMsg.includes('Account limit reached')) {
                window.cliq24App.showAccountLimitError();
            } else {
                window.cliq24App.showError('TikTok connection failed: ' + errorMsg);
            }
        }
    }, 1000);
    window.history.replaceState({}, document.title, window.location.pathname);
}

// Handle subscription success/cancel
const subscriptionStatus = urlParams.get('subscription');
if (subscriptionStatus === 'success') {
    setTimeout(() => {
        if (window.cliq24App) {
            window.cliq24App.showSuccess('🎉 Welcome to Premium! You can now connect unlimited accounts.');
            window.cliq24App.loadSubscriptionStatus();
        }
    }, 1000);
    window.history.replaceState({}, document.title, window.location.pathname);
}

if (subscriptionStatus === 'canceled') {
    setTimeout(() => {
        if (window.cliq24App) {
            window.cliq24App.showInfo('Subscription upgrade was canceled.');
        }
    }, 1000);
    window.history.replaceState({}, document.title, window.location.pathname);
}
