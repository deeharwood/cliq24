// Facebook Dashboard Application
class FacebookDashboard {
    constructor() {
        this.apiBaseUrl = window.location.origin;
        this.jwtToken = this.getJWTFromStorage();
        this.accountId = this.getAccountIdFromUrl();
        this.account = null;
        this.messages = [];
        this.init();
    }

    // ===== INITIALIZATION =====
    async init() {
        if (!this.accountId) {
            this.showError('No Facebook account ID provided');
            setTimeout(() => window.location.href = '/index.html', 2000);
            return;
        }

        this.setupEventListeners();
        await this.loadUserData();
        await this.loadAccountData();
        await this.loadMessages();
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
            console.log(`Loading Facebook account data for ID: ${this.accountId}`);
            const account = await this.apiCall(`/api/social-accounts/${this.accountId}`);

            if (account) {
                this.account = account;
                this.updateAccountUI(account);
            }
        } catch (error) {
            console.error('Failed to load account data:', error);
            this.showError('Failed to load Facebook account data');
        }
    }

    async loadMessages() {
        try {
            console.log(`Loading messages for account: ${this.accountId}`);
            const messages = await this.apiCall(`/api/facebook/${this.accountId}/messages`);

            if (messages && messages.length > 0) {
                this.messages = messages;
                this.renderMessages(messages);
            } else {
                this.renderEmptyMessages();
            }
        } catch (error) {
            console.error('Failed to load messages:', error);
            this.renderEmptyMessages('Failed to load messages');
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
            await this.loadMessages();
        } catch (error) {
            console.error('Sync failed:', error);
            this.showError('Failed to sync account');
        } finally {
            syncBtn.disabled = false;
            syncIcon.style.animation = '';
        }
    }

    async sendMessage(recipientId, message) {
        try {
            const response = await this.apiCall(`/api/facebook/${this.accountId}/messages/send`, {
                method: 'POST',
                body: JSON.stringify({
                    recipientId: recipientId,
                    message: message
                })
            });

            this.showSuccess('Message sent successfully!');

            // Clear form
            document.getElementById('recipientId').value = '';
            document.getElementById('messageText').value = '';

            // Reload messages
            await this.loadMessages();

            return response;
        } catch (error) {
            console.error('Failed to send message:', error);
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

        // Update "View on Facebook" button link
        const viewOnFacebookBtn = document.getElementById('viewOnFacebookBtn');
        if (viewOnFacebookBtn && account.platformUserId) {
            // Facebook page URL format: https://www.facebook.com/{page-id}
            viewOnFacebookBtn.href = `https://www.facebook.com/${account.platformUserId}`;
        }

        // Update metrics
        const metrics = account.metrics || {};

        document.getElementById('engagementScore').textContent = metrics.engagementScore || '0';
        document.getElementById('followersCount').textContent = this.formatNumber(metrics.connections || 0);
        document.getElementById('postsCount').textContent = this.formatNumber(metrics.posts || 0);
        document.getElementById('messagesCount').textContent = this.formatNumber(metrics.newMessages || 0);

        // Update metric changes (placeholder - would come from API in real app)
        this.updateMetricChange('engagementChange', '+5%');
        this.updateMetricChange('followersChange', '+123');
        this.updateMetricChange('postsChange', '+5');
        this.updateMetricChange('messagesChange', metrics.newMessages || 0);
    }

    updateMetricChange(elementId, value) {
        const el = document.getElementById(elementId);
        if (el) {
            el.querySelector('span:last-child').textContent = value;
        }
    }

    renderMessages(messages) {
        const messagesList = document.getElementById('messagesList');

        if (!messages || messages.length === 0) {
            this.renderEmptyMessages();
            return;
        }

        // Take last 5 messages
        const recentMessages = messages.slice(0, 5);

        messagesList.innerHTML = recentMessages.map(msg => `
            <div class="message-item">
                <div class="message-header">
                    <span class="message-sender">${this.escapeHtml(msg.senderName || msg.senderId || 'Unknown')}</span>
                    <span class="message-time">${this.formatTimeAgo(msg.timestamp)}</span>
                </div>
                <div class="message-text">${this.escapeHtml(msg.message || '')}</div>
            </div>
        `).join('');
    }

    renderEmptyMessages(errorMessage) {
        const messagesList = document.getElementById('messagesList');
        messagesList.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">📭</div>
                <div>${errorMessage || 'No messages yet'}</div>
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

        // Send message form
        const sendForm = document.getElementById('sendMessageForm');
        if (sendForm) {
            sendForm.addEventListener('submit', async (e) => {
                e.preventDefault();

                const recipientId = document.getElementById('recipientId').value.trim();
                const messageText = document.getElementById('messageText').value.trim();

                if (!recipientId || !messageText) {
                    this.showError('Please fill in all fields');
                    return;
                }

                const sendBtn = document.getElementById('sendBtn');
                const originalText = sendBtn.textContent;

                try {
                    sendBtn.disabled = true;
                    sendBtn.innerHTML = '<span class="loading-spinner"></span> Sending...';

                    await this.sendMessage(recipientId, messageText);
                } catch (error) {
                    this.showError(error.message || 'Failed to send message');
                } finally {
                    sendBtn.disabled = false;
                    sendBtn.textContent = originalText;
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
    window.facebookDashboard = new FacebookDashboard();
});
