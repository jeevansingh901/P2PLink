'use client';

import { useState } from 'react';
import { FiDownload } from 'react-icons/fi';

interface FileDownloadProps {
    onDownload: (fileId: string, passphrase?: string) => Promise<void>;
    isDownloading: boolean;
}

export default function FileDownload({ onDownload, isDownloading }: FileDownloadProps) {
    const [fileId, setFileId] = useState('');
    const [passphrase, setPassphrase] = useState('');

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!fileId.trim()) return;
        await onDownload(fileId.trim(), passphrase || undefined);
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-4">
            <div>
                <label className="block text-sm font-medium mb-1">Invite Code</label>
                <input
                    type="text"
                    value={fileId}
                    onChange={(e) => setFileId(e.target.value)}
                    placeholder="Enter the 6-digit code"
                    className="input-field w-full"
                    disabled={isDownloading}
                    required
                />
            </div>

            <div>
                <label className="block text-sm font-medium mb-1">Passphrase (if required)</label>
                <input
                    type="text"
                    value={passphrase}
                    onChange={(e) => setPassphrase(e.target.value)}
                    placeholder="Enter passphrase if needed"
                    className="input-field w-full"
                    disabled={isDownloading}
                />
            </div>

            <button
                type="submit"
                className="btn-primary flex items-center justify-center w-full"
                disabled={isDownloading}
            >
                {isDownloading ? (
                    <span>Downloading...</span>
                ) : (
                    <>
                        <FiDownload className="mr-2" />
                        <span>Download File</span>
                    </>
                )}
            </button>
        </form>
    );
}
