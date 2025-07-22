/**
 * Enhanced chat functionality with better error handling and features
 */

class ChatApp {
    constructor() {
        this.sessionId = this.generateSessionId();
        this.isLoading = false;
        this.messageHistory = [];
        this.currentChart = null;
        
        this.init();
    }
    
    init() {
        this.bindEvents();
        this.loadSuggestions();
        this.setupKeyboardShortcuts();
        this.restoreSession();
        
        // Focus input
        document.getElementById('messageInput').focus();
    }
    
    generateSessionId() {
        return 'session-' + Math.random().toString(36).substr(2, 9);
    }
    
    bindEvents() {
        // Form submission
        document.getElementById('chatForm').addEventListener('submit', (e) => {
            e.preventDefault();
            this.sendMessage();
        });
        
        // Input field - Enter to send, Shift+Enter for new line
        document.getElementById('messageInput').addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendMessage();
            }
        });
        
        // Clear chat button (if exists)
        const clearBtn = document.getElementById('clearChat');
        if (clearBtn) {
            clearBtn.addEventListener('click', () => this.clearChat());
        }
    }
    
    setupKeyboardShortcuts() {
        document.addEventListener('keydown', (e) => {
            // Ctrl/Cmd + K to focus input
            if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
                e.preventDefault();
                document.getElementById('messageInput').focus();
            }
            
            // Escape to cancel loading
            if (e.key === 'Escape' && this.isLoading) {
                this.cancelLoading();
            }
        });
    }
    
    async loadSuggestions() {
        try {
            const response = await fetch('/api/chat/suggestions');
            const suggestions = await response.json();
            
            const container = document.getElementById('suggestions');
            container.innerHTML = suggestions.map(s => 
                `<div class="suggestion" tabindex="0" role="button" 
                      onclick="chatApp.useSuggestion('${this.escapeHtml(s)}')"
                      onkeypress="if(event.key==='Enter') chatApp.useSuggestion('${this.escapeHtml(s)}')">
                    ${this.escapeHtml(s)}
                </div>`
            ).join('');
        } catch (error) {
            console.error('Failed to load suggestions:', error);
        }
    }
    
    useSuggestion(text) {
        document.getElementById('messageInput').value = text;
        this.sendMessage();
    }
    
    async sendMessage() {
        if (this.isLoading) return;
        
        const input = document.getElementById('messageInput');
        const message = input.value.trim();
        
        if (!message) return;
        
        // Add to history
        this.messageHistory.push(message);
        this.saveSession();
        
        // Clear input
        input.value = '';
        
        // Add user message
        this.addMessage('user', message);
        
        // Send to server
        await this.processMessage(message);
    }
    
    async processMessage(message) {
        this.isLoading = true;
        this.updateSendButton(true);
        
        // Add loading indicator
        const loadingId = this.addLoadingMessage();
        
        try {
            const response = await fetch('/api/chat/message', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    sessionId: this.sessionId,
                    message: message
                })
            });
            
            if (!response.ok) {
                throw new Error(`Server error: ${response.status}`);
            }
            
            const data = await response.json();
            
            // Remove loading message
            this.removeMessage(loadingId);
            
            // Add bot response
            this.addBotMessage(data);
            
            // Update session ID if provided
            if (data.sessionId) {
                this.sessionId = data.sessionId;
            }
            
        } catch (error) {
            console.error('Error:', error);
            
            // Remove loading message
            this.removeMessage(loadingId);
            
            // Add error message
            this.addMessage('bot', this.getErrorMessage(error));
        } finally {
            this.isLoading = false;
            this.updateSendButton(false);
        }
    }
    
    addMessage(type, content, data = null) {
        const messagesDiv = document.getElementById('messages');
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${type}`;
        messageDiv.setAttribute('role', 'article');
        
        const timestamp = new Date().toLocaleTimeString([], { 
            hour: '2-digit', 
            minute: '2-digit' 
        });
        
        const avatar = type === 'user' ? 'You' : 'TAS';
        
        messageDiv.innerHTML = `
            <div class="message-avatar" aria-label="${avatar}">${avatar}</div>
            <div class="message-content">
                <div class="message-text">${this.escapeHtml(content)}</div>
                <time class="message-time" datetime="${new Date().toISOString()}">${timestamp}</time>
            </div>
        `;
        
        messagesDiv.appendChild(messageDiv);
        this.scrollToBottom();
        
        return messageDiv.id = 'msg-' + Date.now();
    }
    
    addBotMessage(data) {
        const messagesDiv = document.getElementById('messages');
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message bot';
        messageDiv.setAttribute('role', 'article');
        
        const timestamp = new Date().toLocaleTimeString([], { 
            hour: '2-digit', 
            minute: '2-digit' 
        });
        
        let content = `
            <div class="message-avatar" aria-label="TAS">TAS</div>
            <div class="message-content">
                <div class="message-text">${this.formatMessage(data.message)}</div>
        `;
        
        // Add insights
        if (data.insights) {
            content += this.renderInsights(data.insights);
        }
        
        // Add chart
        if (data.chartData && data.chartData.labels) {
            const chartId = 'chart-' + Date.now();
            content += `
                <div class="chart-container" aria-label="Data visualization">
                    <canvas id="${chartId}" width="400" height="200"></canvas>
                </div>
            `;
            setTimeout(() => this.renderChart(chartId, data.chartData), 100);
        }
        
        // Add query results table (if needed)
        if (data.queryResult && data.queryResult.rows && data.queryResult.rows.length > 0) {
            content += this.renderQueryResults(data.queryResult);
        }
        
        // Add follow-up suggestions
        if (data.followUpSuggestions && data.followUpSuggestions.length > 0) {
            content += this.renderFollowUpSuggestions(data.followUpSuggestions);
        }
        
        content += `
                <time class="message-time" datetime="${new Date().toISOString()}">${timestamp}</time>
            </div>
        `;
        
        messageDiv.innerHTML = content;
        messagesDiv.appendChild(messageDiv);
        this.scrollToBottom();
    }
    
    renderInsights(insights) {
        let html = '';
        
        if (insights.keyFindings && insights.keyFindings.length > 0) {
            html += `
                <div class="insights-section">
                    <h4>📊 Key Findings:</h4>
                    ${insights.keyFindings.map(f => 
                        `<div class="finding">• ${this.escapeHtml(f)}</div>`
                    ).join('')}
                </div>
            `;
        }
        
        if (insights.recommendations && insights.recommendations.length > 0) {
            html += `
                <div class="insights-section">
                    <h4>💡 Recommendations:</h4>
                    ${insights.recommendations.map(r => 
                        `<div class="finding">• ${this.escapeHtml(r)}</div>`
                    ).join('')}
                </div>
            `;
        }
        
        if (insights.trends && Object.keys(insights.trends).length > 0) {
            html += `
                <div class="insights-section">
                    <h4>📈 Trends:</h4>
                    ${Object.entries(insights.trends).map(([key, value]) => 
                        `<div class="finding">• ${this.formatTrendName(key)}: ${this.formatTrendValue(value)}</div>`
                    ).join('')}
                </div>
            `;
        }
        
        return html;
    }
    
    renderQueryResults(result) {
        if (!result.columns || !result.rows || result.rows.length === 0) {
            return '';
        }
        
        // Limit display to 10 rows for UI
        const displayRows = result.rows.slice(0, 10);
        const hasMore = result.rows.length > 10;
        
        let html = `
            <div class="query-results">
                <details>
                    <summary>View Raw Data (${result.rows.length} rows)</summary>
                    <table>
                        <thead>
                            <tr>
                                ${result.columns.map(col => 
                                    `<th>${this.escapeHtml(col)}</th>`
                                ).join('')}
                            </tr>
                        </thead>
                        <tbody>
                            ${displayRows.map(row => `
                                <tr>
                                    ${row.map(cell => 
                                        `<td>${this.escapeHtml(String(cell || ''))}</td>`
                                    ).join('')}
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                    ${hasMore ? `<p><em>Showing first 10 of ${result.rows.length} rows</em></p>` : ''}
                </details>
            </div>
        `;
        
        return html;
    }
    
    renderFollowUpSuggestions(suggestions) {
        return `
            <div class="recommendations" role="group" aria-label="Follow-up suggestions">
                ${suggestions.map(s => `
                    <span class="recommendation" 
                          tabindex="0" 
                          role="button"
                          onclick="chatApp.useSuggestion('${this.escapeHtml(s)}')"
                          onkeypress="if(event.key==='Enter') chatApp.useSuggestion('${this.escapeHtml(s)}')">
                        ${this.escapeHtml(s)}
                    </span>
                `).join('')}
            </div>
        `;
    }
    
    renderChart(canvasId, chartData) {
        const ctx = document.getElementById(canvasId);
        if (!ctx) return;
        
        // Destroy previous chart if exists
        if (this.currentChart) {
            this.currentChart.destroy();
        }
        
        // Configure chart options
        const options = {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                title: {
                    display: !!chartData.title,
                    text: chartData.title
                },
                legend: {
                    display: chartData.datasets && chartData.datasets.length > 1
                },
                tooltip: {
                    mode: 'index',
                    intersect: false
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function(value) {
                            if (Math.floor(value) === value) {
                                return value;
                            }
                        }
                    }
                }
            }
        };
        
        // Create chart
        this.currentChart = new Chart(ctx, {
            type: chartData.chartType || 'bar',
            data: {
                labels: chartData.labels,
                datasets: chartData.datasets.map((ds, index) => ({
                    ...ds,
                    borderWidth: 1,
                    backgroundColor: ds.backgroundColor || this.getChartColor(index, 0.8),
                    borderColor: ds.borderColor || this.getChartColor(index, 1)
                }))
            },
            options: options
        });
    }
    
    getChartColor(index, alpha) {
        const colors = [
            `rgba(52, 152, 219, ${alpha})`,   // Blue
            `rgba(46, 204, 113, ${alpha})`,   // Green
            `rgba(231, 76, 60, ${alpha})`,    // Red
            `rgba(241, 196, 15, ${alpha})`,   // Yellow
            `rgba(155, 89, 182, ${alpha})`,   // Purple
            `rgba(52, 73, 94, ${alpha})`,     // Dark Blue
            `rgba(230, 126, 34, ${alpha})`,   // Orange
            `rgba(149, 165, 166, ${alpha})`   // Gray
        ];
        return colors[index % colors.length];
    }
    
    addLoadingMessage() {
        const messagesDiv = document.getElementById('messages');
        const loadingDiv = document.createElement('div');
        loadingDiv.id = 'loading-' + Date.now();
        loadingDiv.className = 'message bot';
        loadingDiv.innerHTML = `
            <div class="message-avatar">TAS</div>
            <div class="message-content">
                <div class="typing-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                </div>
                <span class="loading-text">Analyzing your query...</span>
            </div>
        `;
        messagesDiv.appendChild(loadingDiv);
        this.scrollToBottom();
        return loadingDiv.id;
    }
    
    removeMessage(messageId) {
        const element = document.getElementById(messageId);
        if (element) {
            element.remove();
        }
    }
    
    updateSendButton(loading) {
        const button = document.getElementById('sendButton');
        button.disabled = loading;
        button.textContent = loading ? 'Sending...' : 'Send';
    }
    
    scrollToBottom() {
        const messagesDiv = document.getElementById('messages');
        messagesDiv.scrollTop = messagesDiv.scrollHeight;
    }
    
    clearChat() {
        if (confirm('Are you sure you want to clear the chat history?')) {
            document.getElementById('messages').innerHTML = '';
            this.messageHistory = [];
            this.sessionId = this.generateSessionId();
            this.saveSession();
            this.addWelcomeMessage();
        }
    }
    
    addWelcomeMessage() {
        this.addMessage('bot', 
            '👋 Hello! I\'m your TAS Query Assistant. How can I help you explore your Time and Attendance System data today?'
        );
    }
    
    saveSession() {
        const sessionData = {
            sessionId: this.sessionId,
            messageHistory: this.messageHistory.slice(-10) // Keep last 10 messages
        };
        localStorage.setItem('tasQuerySession', JSON.stringify(sessionData));
    }
    
    restoreSession() {
        try {
            const saved = localStorage.getItem('tasQuerySession');
            if (saved) {
                const data = JSON.parse(saved);
                this.sessionId = data.sessionId || this.sessionId;
                this.messageHistory = data.messageHistory || [];
            }
        } catch (error) {
            console.error('Failed to restore session:', error);
        }
    }
    
    getErrorMessage(error) {
        if (error.message.includes('Failed to fetch')) {
            return 'Unable to connect to the server. Please check your connection and try again.';
        } else if (error.message.includes('500')) {
            return 'Server error occurred. Please try rephrasing your query or contact support.';
        } else if (error.message.includes('timeout')) {
            return 'Request timed out. Please try a simpler query.';
        }
        return 'Sorry, an unexpected error occurred. Please try again.';
    }
    
    formatMessage(text) {
        // Convert markdown-style formatting
        return text
            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
            .replace(/\*(.*?)\*/g, '<em>$1</em>')
            .replace(/`(.*?)`/g, '<code>$1</code>')
            .replace(/\n/g, '<br>');
    }
    
    formatTrendName(name) {
        return name
            .replace(/_/g, ' ')
            .replace(/\b\w/g, l => l.toUpperCase());
    }
    
    formatTrendValue(value) {
        if (typeof value === 'number') {
            return value.toLocaleString();
        }
        return String(value);
    }
    
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    cancelLoading() {
        // Implement request cancellation if needed
        this.isLoading = false;
        this.updateSendButton(false);
    }
}

// Initialize app when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    window.chatApp = new ChatApp();
});

// Global function for onclick handlers
window.sendSuggestion = function(text) {
    if (window.chatApp) {
        window.chatApp.useSuggestion(text);
    }
};