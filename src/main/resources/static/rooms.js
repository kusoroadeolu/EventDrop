
class EventDropRoomManager {
    constructor() {
        this.baseUrl = window.location.origin;
        this.eventSource = null;
        this.currentRoomState = null;

        // Bind methods to preserve context
        this.handleSSEMessage = this.handleSSEMessage.bind(this);
        this.handleSSEError = this.handleSSEError.bind(this);

        this.init();
    }

    /**
     * Initialize the room manager
     */
    init() {
        console.log('Initializing EventDrop Room Manager...');
        this.attachEventListeners();
        this.connectSSE();
    }

    /**
     * Get username from URL parameters (passed from create/join page)
     */
    getUsername() {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get('username');
    }

    /**
     * Attach event listeners to all interactive elements
     */
    attachEventListeners() {
        // File upload button
        const uploadBtn = document.querySelector('button[class*="btn-primary"]');
        if (uploadBtn) {
            uploadBtn.addEventListener('click', () => this.handleFileUpload());
        }

        // Delete room button
        const deleteRoomBtn = document.querySelector('button[class*="btn-danger"]');
        if (deleteRoomBtn) {
            deleteRoomBtn.addEventListener('click', () => this.handleDeleteRoom());
        }

        // Leave room button
        const leaveRoomBtn = document.querySelector('button[class*="btn-neutral"]');
        if (leaveRoomBtn) {
            leaveRoomBtn.addEventListener('click', () => this.handleLeaveRoom());
        }

        // File action buttons (download/delete) - delegated event handling
        const filesTable = document.querySelector('.files-table tbody');
        if (filesTable) {
            filesTable.addEventListener('click', (e) => {
                if (e.target.closest('.btn-download')) {
                    const fileId = this.getFileIdFromRow(e.target);
                    if (fileId) {
                        this.handleFileDownload(fileId);
                    }
                } else if (e.target.closest('.btn-delete')) {
                    const fileId = this.getFileIdFromRow(e.target);
                    if (fileId) {
                        this.handleFileDelete(fileId);
                    }
                }
            });
        }

        console.log('Event listeners attached');
    }

    /**
     * Connect to Server-Sent Events endpoint
     */
    connectSSE() {
        const sseUrl = `${this.baseUrl}/rooms`;
        console.log(`Connecting to SSE endpoint: ${sseUrl}`);

        try {
            this.eventSource = new EventSource(sseUrl, {
                withCredentials: true
            });

            this.eventSource.onmessage = this.handleSSEMessage;
            this.eventSource.onerror = this.handleSSEError;
            this.eventSource.onopen = () => {
                console.log('SSE connection established');
            };

        } catch (error) {
            console.error('Failed to establish SSE connection:', error);
            this.showNotification('Failed to connect to room updates', 'error');
        }
    }

    /**
     * Handle incoming SSE messages
     */
    handleSSEMessage(event) {
        try {
            const roomState = JSON.parse(event.data);

            if (roomState.isExpired) {
                // Room expired
                this.showNotification('Room has expired. Redirecting...', 'error');
                setTimeout(() => {
                    this.cleanup();
                    window.location.href = '/create.html';
                }, 1000);
                return;
            }

            console.log('Received room state update:', roomState);

            this.currentRoomState = roomState;
            this.updateRoomUI(roomState);

            if (roomState.notification) {
                this.showNotification(roomState.notification, 'info');
            }
        } catch (error) {
            console.error('Failed to parse SSE message:', error);
        }
    }

    /**
     * Handle SSE connection errors
     */
    handleSSEError(error) {
        console.error('SSE connection error:', error);

        console.error('SSE connection error:', error);

            // Check if the connection is closed.
            if (this.eventSource.readyState === EventSource.CLOSED) {
                console.log('SSE connection closed. Assuming room has expired or access is denied.');
                this.showNotification('Room has expired or access denied. Redirecting...', 'error');

                // Clean up and redirect the user.
                this.cleanup();
                setTimeout(() => {
                    window.location.href = '/create.html';
                }, 1000);
            }

//        if (this.eventSource.readyState === EventSource.CLOSED) {
//            console.log('SSE connection closed, attempting to reconnect...');
//            setTimeout(() => this.connectSSE(), 5000);
//        }
    }

    /**
     * Update room UI with new state data
     */
    updateRoomUI(roomState) {
        // Update room info panel
        this.updateRoomInfo(roomState);

        // Update files list
        this.updateFilesList(roomState.fileDrops);
    }

    /**
     * Update room information panel
     */
    updateRoomInfo(roomState) {
        // Update room code (main title)
        const roomNameEl = document.querySelector('.room-name');
        if (roomNameEl && roomState.roomCode) {
            roomNameEl.textContent = roomState.roomCode;
        }

        // Update username (first detail)
        const usernameEl = document.querySelector('#username-display');
        if (usernameEl) {
            const username = this.getUsername();
            if (username) {
                usernameEl.textContent = username;
            }
        }

        // Update room name (second detail - room code section)
        const roomCodeEl = document.querySelector('.room-detail:nth-child(2) span');
        if (roomCodeEl && roomState.roomName) {
            roomCodeEl.textContent = roomState.roomName;
        }

        // Update expiration date (third detail)
        const expirationEl = document.querySelector('.room-detail:nth-child(3) span');
        if (expirationEl && roomState.expiresAt) {
            const expireDate = new Date(roomState.expiresAt);
            expirationEl.textContent = `Expires on ${expireDate.toLocaleDateString('en-US', {
                year: 'numeric',
                month: 'long',
                day: 'numeric'
            })}`;
        }

        // Update occupant count (fourth detail)
        const occupantEl = document.querySelector('.room-detail:nth-child(4) span');
        if (occupantEl && roomState.occupantCount !== undefined) {
            occupantEl.textContent = `${roomState.occupantCount} people in this room`;
        }
    }

    /**
     * Update files list table
     */
    updateFilesList(fileDrops) {
        const tbody = document.querySelector('.files-table tbody');
        if (!tbody) {
            console.error('Files table tbody not found');
            return;
        }

        console.log('Updating files list with:', fileDrops);

        // Clear existing rows
        tbody.innerHTML = '';

        if (!fileDrops || fileDrops.length === 0) {
            console.log('No files to display');
            tbody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: #999;">No files uploaded yet</td></tr>';
            return;
        }

        console.log('Processing', fileDrops.length, 'files');
        fileDrops.forEach((file, index) => {
            console.log(`Processing file ${index + 1}:`, file);
            try {
                const row = this.createFileRow(file);
                tbody.appendChild(row);
                console.log(`Successfully created row for: ${file.fileName}`);
            } catch (error) {
                console.error(`Error creating row for file ${file.fileName}:`, error);
            }
        });

        console.log('Files list update completed');
    }

    /**
     * Create a table row for a file
     */
    createFileRow(file) {
        const row = document.createElement('tr');
        row.dataset.fileId = file.fileId;

        const fileIcon = this.getFileIcon(file.fileName);
        const fileSize = this.formatFileSize(file.fileSizeInBytes);
        const uploadTime = this.formatUploadTime(file.uploadedAt);

        row.innerHTML = `
            <td>
                <div class="file-name">
                    <i class="${fileIcon.class} ${fileIcon.colorClass}"></i>
                    <span>${file.fileName}</span>
                </div>
            </td>
            <td>${fileSize}</td>
            <td>${uploadTime}</td>
            <td>
                <div class="action-buttons">
                    <button class="btn-icon btn-download" title="Download file">
                        <i class="ri-download-line"></i>
                    </button>
                    <button class="btn-icon btn-delete" title="Delete file">
                        <i class="ri-delete-bin-line"></i>
                    </button>
                </div>
            </td>
        `;

        return row;
    }

    /**
     * Create file action buttons (always show all buttons)
     */
    createFileActionButtons() {
        return `
            <button class="btn-icon btn-download" title="Download file">
                <i class="ri-download-line"></i>
            </button>
            <button class="btn-icon btn-delete" title="Delete file">
                <i class="ri-delete-bin-line"></i>
            </button>
        `;
    }

    getFileIcon(fileName) {
        const extension = fileName.toLowerCase().split('.').pop();

        const iconMap = {
            'pdf': { class: 'ri-file-pdf-line', colorClass: 'file-icon-pdf' },
            'doc': { class: 'ri-file-word-line', colorClass: 'file-icon-doc' },
            'docx': { class: 'ri-file-word-line', colorClass: 'file-icon-doc' },
            'xls': { class: 'ri-file-excel-line', colorClass: 'file-icon-xls' },
            'xlsx': { class: 'ri-file-excel-line', colorClass: 'file-icon-xls' },
            'png': { class: 'ri-image-line', colorClass: 'file-icon-img' },
            'jpg': { class: 'ri-image-line', colorClass: 'file-icon-img' },
            'jpeg': { class: 'ri-image-line', colorClass: 'file-icon-img' },
            'gif': { class: 'ri-image-line', colorClass: 'file-icon-img' },
            'txt': { class: 'ri-file-text-line', colorClass: 'file-icon-doc' }
        };

        return iconMap[extension] || { class: 'ri-file-line', colorClass: 'file-icon-doc' };
    }

    /**
     * Format file size from bytes to human readable
     */
    formatFileSize(bytes) {
        if (bytes === 0) return '0 B';

        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));

        return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
    }

    /**
     * Format upload time to absolute date and time
     */
    formatUploadTime(uploadedAt) {
        const uploadDate = new Date(uploadedAt);

        return uploadDate.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        }) + ' at ' + uploadDate.toLocaleTimeString('en-US', {
            hour: 'numeric',
            minute: '2-digit',
            hour12: true
        });
    }

    /**
     * Handle file upload
     */
    async handleFileUpload() {
        // Create file input element
        const input = document.createElement('input');
        input.type = 'file';
        input.multiple = true;
        input.style.display = 'none';

        input.addEventListener('change', async (e) => {
            const files = Array.from(e.target.files);
            if (files.length === 0) return;

            try {
                if (files.length === 1) {
                    await this.uploadSingleFile(files[0]);
                } else {
                    await this.uploadMultipleFiles(files);
                }
            } catch (error) {
                console.error('File upload failed:', error);
                this.showNotification('File upload failed', 'error');
            }
        });

        document.body.appendChild(input);
        input.click();
        document.body.removeChild(input);
    }

    /**
     * Upload a single file
     */
    async uploadSingleFile(file) {
        const uploadNotification = this.showUploadNotification(1);

        const formData = new FormData();
        formData.append('file', file);

        try {
            const response = await fetch(`${this.baseUrl}/files`, {
                method: 'POST',
                body: formData,
                credentials: 'include'
            });

            this.removeUploadNotification();

            if (response.ok) {
                const result = await response.json();
                console.log('File uploaded successfully:', result);
                this.showNotification(`${file.name} uploaded successfully`, 'success');
            } else {
                await this.handleApiError(response, `Failed to upload ${file.name}`);
            }
        } catch (error) {
            this.removeUploadNotification();
            console.error('Single file upload error:', error);
            this.showNotification(`Failed to upload ${file.name}`, 'error');
        }
    }

    /**
     * Upload multiple files
     */
    async uploadMultipleFiles(files) {
        const uploadNotification = this.showUploadNotification(files.length);

        const formData = new FormData();
        files.forEach(file => {
            formData.append('file', file); // Use 'file' to match @RequestParam("file")
        });

        try {
            const response = await fetch(`${this.baseUrl}/files/batch`, {
                method: 'POST',
                body: formData,
                credentials: 'include'
            });

            this.removeUploadNotification();

            if (response.ok) {
                const result = await response.json();
                console.log('Batch upload result:', result);

                if (result.successfulUploads && result.successfulUploads.length > 0) {
                    this.showNotification(`${result.successfulUploads.length} files uploaded successfully`, 'success');
                }

                if (result.failedUploads && result.failedUploads.length > 0) {
                    this.showNotification(`${result.failedUploads.length} files failed to upload`, 'error');
                }
            } else {
                await this.handleApiError(response, 'Batch file upload failed');
            }
        } catch (error) {
            this.removeUploadNotification();
            console.error('Batch file upload error:', error);
            this.showNotification('Batch file upload failed', 'error');
        }
    }

    /**
     * Handle file download - fetch download URL then redirect browser
     */
    async handleFileDownload(fileId) {
        if (!fileId) {
            this.showNotification('Invalid file ID', 'error');
            return;
        }

        console.log('Attempting to download file with ID:', fileId);
        console.log('Download URL:', `${this.baseUrl}/files/${fileId}`);

        try {
            // First, fetch the download URL from your endpoint
            const response = await fetch(`${this.baseUrl}/files/${fileId}`, {
                method: 'GET',
                credentials: 'include'
            });

            console.log('Download response status:', response.status);

            // Handle both successful responses (200-299) and redirects (302)
            if (response.ok || response.status === 302) {
                const result = await response.json();
                console.log('Download response data:', result);
                this.showNotification('Your download should start shortly', 'info')

                if (result.downloadUrl) {
                    console.log('Redirecting to download URL:', result.downloadUrl);
                    // Now redirect browser to the actual download URL
                    window.location.href = result.downloadUrl;
                } else {
                    console.error('No downloadUrl in response:', result);
                    this.showNotification('No download URL provided', 'error');
                }
            } else {
                console.error('Download request failed with status:', response.status);
                const errorText = await response.text();
                console.error('Error response body:', errorText);
                await this.handleApiError(response, 'Failed to get download URL');
            }
        } catch (error) {
            console.error('File download error:', error);
            this.showNotification('Failed to download file', 'error');
        }
    }

    /**
     * Handle file delete for individual files
     */
    async handleFileDelete(fileId) {
        if (!fileId) {
            this.showNotification('Invalid file ID', 'error');
            return;
        }

        // Confirm deletion
        if (!confirm('Are you sure you want to delete this file?')) {
            return;
        }

        try {
            const response = await fetch(`${this.baseUrl}/files`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify([fileId]), // Send as array with single file ID
                credentials: 'include'
            });

            if (response.ok || response.status === 204) { // Handle NO_CONTENT status
                const result = response.status === 204 ? null : await response.json();
                console.log('File deletion result:', result);

                if (!result || (result.successfulDeletes && result.successfulDeletes.length > 0)) {
                    this.showNotification('File deleted successfully', 'success');
                } else if (result.failedDeletes && result.failedDeletes.length > 0) {
                    this.showNotification('Failed to delete file', 'error');
                }
            } else {
                await this.handleApiError(response, 'Failed to delete file');
            }
        } catch (error) {
            console.error('File delete error:', error);
            this.showNotification('Failed to delete file', 'error');
        }
    }

    /**
     * Handle leaving the room
     */
    async handleLeaveRoom() {
        if (!confirm('Are you sure you want to leave this room?')) {
            return;
        }

        try {
            const response = await fetch(`${this.baseUrl}/rooms/leave`, {
                method: 'DELETE',
                credentials: 'include'
            });

            if (response.ok) {
                this.showNotification('Left room successfully', 'success');
                this.cleanup();
                // Redirect to create page after a short delay
                setTimeout(() => {
                    window.location.href = '/create.html';
                }, 500);
            } else {
                await this.handleApiError(response, 'Failed to leave room');
            }
        } catch (error) {
            console.error('Leave room error:', error);
            this.showNotification('Failed to leave room', 'error');
        }
    }

    /**
     * Handle deleting the room
     */
    async handleDeleteRoom() {
        if (!confirm('Are you sure you want to delete this room? This action cannot be undone.')) {
            return;
        }

        try {
            const response = await fetch(`${this.baseUrl}/rooms/delete`, {
                method: 'DELETE',
                credentials: 'include'
            });

            if (response.ok) {
                this.showNotification('Room deleted successfully', 'success');
                this.cleanup();
                // Redirect to create page after a short delay
                setTimeout(() => {
                    window.location.href = '/create.html';
                }, 500);
            } else {
                await this.handleApiError(response, 'Failed to delete room');
            }
        } catch (error) {
            console.error('Delete room error:', error);
            this.showNotification('Failed to delete room', 'error');
        }
    }

    /**
     * Handle API errors and show appropriate messages
     */
    async handleApiError(response, defaultMessage) {
        try {
            const errorData = await response.json();
            let errorMessage = defaultMessage;

            // Handle different status codes
            if (response.status === 403) {
                errorMessage = "You don't have permission to perform this action because you aren't the room owner";
            } else if (response.status === 401) {
                errorMessage = "You are not authenticated. Please refresh the page and try again";
            } else if (response.status === 404) {
                errorMessage = "Resource not found";
            }else if (response.status === 409){
                errorMessage = defaultMessage;
            } else if (response.status === 500) {
                errorMessage = "Server error occurred. Please try again later";
            }else if (response.status === 410) {
                    errorMessage = "This room has expired. Redirecting you back to create room page...";
                    this.showNotification(errorMessage, 'error');
                    // Clean up and redirect after showing the message
                    setTimeout(() => {
                        this.cleanup();
                        window.location.href = '/create.html';
                    }, 500);
            } else if (errorData.message) {
                errorMessage = errorData.message;
            }

            this.showNotification(errorMessage, 'error');
        } catch (parseError) {
            // If we can't parse the error response, use default message
            this.showNotification(defaultMessage, 'error');
        }
    }

    getFileIdFromRow(element) {
        const row = element.closest('tr');
        return row ? row.dataset.fileId : null;
    }

    /**
     * Show notification toast
     */
    showNotification(message, type = 'info') {
        const container = document.querySelector('.notifications-container');
        if (!container) {
            console.warn('Notifications container not found');
            return;
        }

        const notification = document.createElement('div');
        notification.className = 'notification';

        const iconClass = {
            'info': 'ri-information-line',
            'success': 'ri-check-line',
            'error': 'ri-error-warning-line',
            'warning': 'ri-alert-line'
        }[type] || 'ri-information-line';

        notification.innerHTML = `
            <i class="${iconClass}"></i>
            <span>${message}</span>
        `;

        // Add type-specific styling
        if (type === 'error') {
            notification.style.borderLeftColor = 'var(--danger)';
            notification.querySelector('i').style.color = 'var(--danger)';
        } else if (type === 'success') {
            notification.style.borderLeftColor = 'var(--success)';
            notification.querySelector('i').style.color = 'var(--success)';
        } else if (type === 'warning') {
            notification.style.borderLeftColor = 'var(--warning)';
            notification.querySelector('i').style.color = 'var(--warning)';
        }

        container.appendChild(notification);

        // Auto-remove after 4 seconds
        setTimeout(() => {
            if (notification.parentNode) {
                notification.style.animation = 'slideOut 0.3s ease forwards';
                setTimeout(() => {
                    if (notification.parentNode) {
                        container.removeChild(notification);
                    }
                }, 300);
            }
        }, 4000);
    }

    /**
     * Show persistent upload notification that can be removed later
     */
    showUploadNotification(fileCount) {
        const container = document.querySelector('.notifications-container');
        if (!container) {
            console.warn('Notifications container not found');
            return null;
        }

        const notification = document.createElement('div');
        notification.className = 'notification upload-notification';
        notification.id = 'upload-notification';

        const fileText = fileCount === 1 ? '1 file' : `${fileCount} files`;

        notification.innerHTML = `
            <i class="ri-upload-2-line"></i>
            <span>Uploading ${fileText}...</span>
        `;

        // Style as info notification
        notification.style.borderLeftColor = 'var(--primary)';
        notification.querySelector('i').style.color = 'var(--primary)';

        container.appendChild(notification);
        return notification;
    }

    /**
     * Remove upload notification
     */
    removeUploadNotification() {
        const notification = document.getElementById('upload-notification');
        if (notification && notification.parentNode) {
            notification.style.animation = 'slideOut 0.3s ease forwards';
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification);
                }
            }, 300);
        }
    }

    /**
     * Clean up resources
     */
    cleanup() {
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
        }
        console.log('EventDrop Room Manager cleaned up');
    }
}

// Add slideOut animation to CSS if not present
const style = document.createElement('style');
style.textContent = `
    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(100%);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);

// Initialize the room manager when the page loads
document.addEventListener('DOMContentLoaded', () => {
    window.eventDropRoomManager = new EventDropRoomManager();
});

// Clean up when page unloads
window.addEventListener('beforeunload', () => {
    if (window.eventDropRoomManager) {
        window.eventDropRoomManager.cleanup();
    }
});

