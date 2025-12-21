// LinkedIn Dashboard Application
class LinkedInDashboard {
    constructor() {
        this.apiBaseUrl = window.location.origin;
        this.jwtToken = this.getJWTFromStorage();
        this.accountId = this.getAccountIdFromUrl();
        this.account = null;
        this.posts = [];
        this.init();
    }

    // ===== INITIALIZATION =====
    async init() {
        if (!this.accountId) {
            this.showError('No LinkedIn account ID provided');
            setTimeout(() => window.location.href = '/index.html', 2000);
            return;
        }

        this.setupEventListeners();
        await this.loadUserData();
        await this.loadAccountData();
    }

    // ===== UTILITY FUNCTIONS =====
    getJWTFromStorage() {
        try {
            return localStorage.getItem('cliq24_jwt') || sessionStorage.getItem('cliq24_jwt');
        } catch (e) {
            console.warn('localStorage blocked, using sessionStorage:', e);
            try {
                return sessionStorage.getItem('cliq24_jwt');
            } catch (e2) {
                console.error('Both localStorage and sessionStorage blocked:', e2);
                return null;
            }
        }
    }

    getAccountIdFromUrl() {
        const params = new URLSearchParams(window.location.search);
        return params.get('id');
    }

    formatNumber(num) {
        if (!num) return '0';
        if (num >= 1000000) {
            return (num / 1000000).toFixed(1) + 'M';
        }
        if (num >= 1000) {
            return (num / 1000).toFixed(1) + 'K';
        }
        return num.toString();
    }

    formatTimeAgo(timestamp) {
        if (!timestamp) return 'Just now';

        const date = new Date(timestamp);
        const now = new Date();
        const seconds = Math.floor((now - date) / 1000);

        if (seconds < 60) return 'Just now';
        if (seconds < 3600) return Math.floor(seconds / 60) + 'm ago';
        if (seconds < 86400) return Math.floor(seconds / 3600) + 'h ago';
        if (seconds < 604800) return Math.floor(seconds / 86400) + 'd ago';

        return date.toLocaleDateString();
    }

    // ===== API CALLS =====
    async apiCall(endpoint, options = {}) {
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        // Add Authorization header if we have a token
        if (this.jwtToken) {
            headers['Authorization'] = `Bearer ${this.jwtToken}`;
        }

        try {
            console.log(`[API] Calling: ${this.apiBaseUrl}${endpoint}`);
            const response = await fetch(`${this.apiBaseUrl}${endpoint}`, {
                ...options,
                headers
            });

            console.log(`[API] Response status: ${response.status}`);

            if (response.status === 401) {
                this.handleUnauthorized();
                throw new Error('Unauthorized');
            }

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `API Error: ${response.statusText}`);
            }

            return await response.json();
        } catch (error) {
            console.error('[API] Call failed:', error);
            throw error;
        }
    }

    async loadUserData() {
        try {
            const user = await this.apiCall('/auth/me');
            if (user) {
                this.updateUserUI(user);
            }
        } catch (error) {
            console.error('Failed to load user data:', error);
        }
    }

    async loadAccountData() {
        try {
            console.log(`Loading LinkedIn account data for ID: ${this.accountId}`);
            const account = await this.apiCall(`/api/social-accounts/account/${this.accountId}`);

            if (account) {
                this.account = account;
                this.updateAccountUI(account);

                // Show appropriate sections based on account type
                const accountType = account.accountType || 'personal';
                this.renderForAccountType(accountType);

                // Load additional data based on account type
                if (accountType === 'company') {
                    await this.loadPosts();
                } else {
                    // For personal accounts, populate manual metrics form with current values
                    this.populateManualMetricsForm(account);
                }
            }
        } catch (error) {
            console.error('Failed to load account data:', error);
            this.showError('Failed to load LinkedIn account data');
        }
    }

    async loadPosts() {
        try {
            console.log(`Loading posts for LinkedIn company account: ${this.accountId}`);
            const posts = await this.apiCall(`/api/linkedin/${this.accountId}/posts?limit=10`);

            if (posts && posts.length > 0) {
                this.posts = posts;
                this.renderPosts(posts);
            } else {
                this.renderEmptyPosts();
            }
        } catch (error) {
            console.error('Failed to load posts:', error);
            this.renderEmptyPosts('Failed to load posts');
        }
    }

    async syncAccount() {
        const syncBtn = document.getElementById('syncBtn');
        const syncIcon = document.getElementById('syncIcon');

        try {
            syncBtn.disabled = true;
            syncIcon.style.animation = 'spin 1s linear infinite';

            await this.apiCall(`/api/social-accounts/${this.accountId}/sync`, {
                method: 'POST'
            });

            this.showSuccess('Account synced successfully!');
            await this.loadAccountData();

            // Reload posts for company accounts
            if (this.account && this.account.accountType === 'company') {
                await this.loadPosts();
            }
        } catch (error) {
            console.error('Sync failed:', error);
            this.showError('Failed to sync account');
        } finally {
            syncBtn.disabled = false;
            syncIcon.style.animation = '';
        }
    }

    async updateManualMetrics(metrics) {
        try {
            const response = await this.apiCall(`/api/linkedin/${this.accountId}/manual-metrics`, {
                method: 'POST',
                body: JSON.stringify(metrics)
            });

            this.showSuccess('Metrics updated successfully!');

            // Reload account data to get updated engagement score
            await this.loadAccountData();

            return response;
        } catch (error) {
            console.error('Failed to update manual metrics:', error);
            throw error;
        }
    }

    // ===== UI UPDATES =====
    updateUserUI(user) {
        const userName = document.getElementById('userName');
        const userAvatar = document.getElementById('userAvatar');

        if (userName) {
            userName.textContent = user.name || user.email;
        }

        if (userAvatar && user.picture) {
            userAvatar.style.backgroundImage = `url(${user.picture})`;
            userAvatar.style.backgroundSize = 'cover';
            userAvatar.style.backgroundPosition = 'center';
        }
    }

    updateAccountUI(account) {
        // Update username
        const usernameEl = document.getElementById('accountUsername');
        if (usernameEl) {
            usernameEl.textContent = '@' + (account.username || 'unknown');
        }

        // Update account type badge
        const accountTypeBadge = document.getElementById('accountTypeBadge');
        if (accountTypeBadge) {
            const accountType = account.accountType || 'personal';
            accountTypeBadge.textContent = accountType === 'company' ? 'Company Page' : 'Personal';
        }

        // Update "View on LinkedIn" button link
        const viewOnLinkedInBtn = document.getElementById('viewOnLinkedInBtn');
        if (viewOnLinkedInBtn && account.platformUserId) {
            // LinkedIn profile URL format
            if (account.accountType === 'company') {
                viewOnLinkedInBtn.href = `https://www.linkedin.com/company/${account.platformUserId}`;
            } else {
                viewOnLinkedInBtn.href = `https://www.linkedin.com/in/${account.username || account.platformUserId}`;
            }
        }

        // Update metrics
        const metrics = account.metrics || {};
        const accountType = account.accountType || 'personal';

        if (accountType === 'company') {
            // Show company page metrics
            document.getElementById('engagementScore').textContent = metrics.engagementScore || '0';
            document.getElementById('followersCount').textContent = this.formatNumber(metrics.connections || 0); // connections = followers for company
            document.getElementById('postsCount').textContent = this.formatNumber(metrics.posts || 0);
            document.getElementById('growthCount').textContent = this.formatNumber(metrics.followersGained || 0);

            // Update metric changes
            this.updateMetricChange('engagementChange', '+' + (metrics.engagementScore || 0) + ' pts');
            this.updateMetricChange('followersChange', '+' + this.formatNumber(metrics.followersGained || 0));
            this.updateMetricChange('postsChange', 'Last 30 days');
            this.updateMetricChange('growthChange', 'Last 30 days');

            // Update label
            const followersLabel = document.getElementById('followersLabel');
            if (followersLabel) {
                followersLabel.textContent = 'Followers';
            }
        } else {
            // Personal profile - show limited data message
            document.getElementById('engagementScore').textContent = 'N/A';
            document.getElementById('followersCount').textContent = 'N/A';
            document.getElementById('postsCount').textContent = 'N/A';
            document.getElementById('growthCount').textContent = 'N/A';

            // Hide metrics grid for personal profiles
            const metricsGrid = document.getElementById('metricsGrid');
            if (metricsGrid) {
                metricsGrid.style.display = 'none';
            }
        }
    }

    updateMetricChange(elementId, value) {
        const el = document.getElementById(elementId);
        if (el) {
            el.querySelector('span:last-child').textContent = value;
        }
    }

    renderForAccountType(accountType) {
        const manualMetricsSection = document.getElementById('manualMetricsSection');
        const postsSection = document.getElementById('postsSection');

        if (accountType === 'company') {
            // Company account: show posts, hide manual metrics
            if (manualMetricsSection) manualMetricsSection.style.display = 'none';
            if (postsSection) postsSection.style.display = 'block';
        } else {
            // Personal account: show manual metrics, hide posts
            if (manualMetricsSection) manualMetricsSection.style.display = 'block';
            if (postsSection) postsSection.style.display = 'none';
        }
    }

    populateManualMetricsForm(account) {
        const manualMetrics = account.manualMetrics || {};

        document.getElementById('connectionsInput').value = manualMetrics.connections || '';
        document.getElementById('postsInput').value = manualMetrics.posts || '';
        document.getElementById('pendingInput').value = manualMetrics.pendingResponses || '';
        document.getElementById('messagesInput').value = manualMetrics.newMessages || '';
    }

    renderPosts(posts) {
        const postsList = document.getElementById('postsList');

        if (!posts || posts.length === 0) {
            this.renderEmptyPosts();
            return;
        }

        postsList.innerHTML = posts.map(post => `
            <div class="post-card">
                <div class="post-message">${this.escapeHtml(post.text || post.message || 'No content')}</div>
                <div class="post-time">${this.formatTimeAgo(post.createdAt || post.created_time)}</div>

                ${post.impressionCount !== undefined ? `
                    <div class="post-metrics">
                        <div class="post-metric">
                            <span class="metric-icon">👁️</span>
                            <span class="metric-value">${this.formatNumber(post.impressionCount || 0)}</span>
                            <span class="metric-label">Impressions</span>
                        </div>
                        <div class="post-metric">
                            <span class="metric-icon">👍</span>
                            <span class="metric-value">${this.formatNumber(post.likeCount || 0)}</span>
                            <span class="metric-label">Likes</span>
                        </div>
                        <div class="post-metric">
                            <span class="metric-icon">💬</span>
                            <span class="metric-value">${this.formatNumber(post.commentCount || 0)}</span>
                            <span class="metric-label">Comments</span>
                        </div>
                        <div class="post-metric">
                            <span class="metric-icon">🔄</span>
                            <span class="metric-value">${this.formatNumber(post.shareCount || 0)}</span>
                            <span class="metric-label">Shares</span>
                        </div>
                        <div class="post-metric highlight">
                            <span class="metric-icon">📈</span>
                            <span class="metric-value">${post.engagementRate || 0}%</span>
                            <span class="metric-label">Engagement</span>
                        </div>
                    </div>
                ` : ''}
            </div>
        `).join('');
    }

    renderEmptyPosts(errorMessage) {
        const postsList = document.getElementById('postsList');
        postsList.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">📋</div>
                <div>${errorMessage || 'No posts yet. Connect your LinkedIn Company Page for real post analytics.'}</div>
            </div>
        `;
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // ===== EVENT LISTENERS =====
    setupEventListeners() {
        // Logout button
        const logoutBtn = document.getElementById('logoutBtn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', () => this.logout());
        }

        // Sync button
        const syncBtn = document.getElementById('syncBtn');
        if (syncBtn) {
            syncBtn.addEventListener('click', () => this.syncAccount());
        }

        // Manual metrics form
        const manualMetricsForm = document.getElementById('manualMetricsForm');
        if (manualMetricsForm) {
            manualMetricsForm.addEventListener('submit', async (e) => {
                e.preventDefault();

                const connections = parseInt(document.getElementById('connectionsInput').value) || 0;
                const posts = parseInt(document.getElementById('postsInput').value) || 0;
                const pendingResponses = parseInt(document.getElementById('pendingInput').value) || 0;
                const newMessages = parseInt(document.getElementById('messagesInput').value) || 0;

                const metrics = {
                    connections,
                    posts,
                    pendingResponses,
                    newMessages
                };

                const updateBtn = document.getElementById('updateBtn');
                const originalText = updateBtn.textContent;

                try {
                    updateBtn.disabled = true;
                    updateBtn.innerHTML = '<span class="loading-spinner"></span> Updating...';

                    await this.updateManualMetrics(metrics);

                    // Show success message
                    const updateMessage = document.getElementById('updateMessage');
                    updateMessage.innerHTML = `
                        <div class="success-message">
                            <span>✅</span>
                            <span>Metrics updated successfully! Your engagement score has been recalculated.</span>
                        </div>
                    `;

                    // Clear success message after 5 seconds
                    setTimeout(() => {
                        updateMessage.innerHTML = '';
                    }, 5000);
                } catch (error) {
                    this.showError(error.message || 'Failed to update metrics');

                    // Show error message
                    const updateMessage = document.getElementById('updateMessage');
                    updateMessage.innerHTML = `
                        <div class="error-message">
                            <span>❌</span>
                            <span>Failed to update metrics. Please try again.</span>
                        </div>
                    `;

                    setTimeout(() => {
                        updateMessage.innerHTML = '';
                    }, 5000);
                } finally {
                    updateBtn.disabled = false;
                    updateBtn.textContent = originalText;
                }
            });
        }
    }

    // ===== NOTIFICATIONS =====
    showSuccess(message) {
        this.showNotification(message, 'success');
    }

    showError(message) {
        this.showNotification(message, 'error');
    }

    showNotification(message, type = 'info') {
        console.log(`[${type.toUpperCase()}] ${message}`);

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

    handleUnauthorized() {
        localStorage.removeItem('cliq24_jwt');
        sessionStorage.removeItem('cliq24_jwt');
        window.location.href = '/index.html';
    }

    logout() {
        localStorage.removeItem('cliq24_jwt');
        sessionStorage.removeItem('cliq24_jwt');
        this.showSuccess('Logged out successfully');
        setTimeout(() => {
            window.location.href = '/index.html';
        }, 1000);
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

    @keyframes spin {
        to { transform: rotate(360deg); }
    }
`;
document.head.appendChild(style);

// ===== INITIALIZE APP =====
document.addEventListener('DOMContentLoaded', () => {
    window.linkedInDashboard = new LinkedInDashboard();
});
