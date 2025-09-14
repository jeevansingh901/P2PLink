'use client';

import { useState } from 'react';
import { FiUpload } from 'react-icons/fi';

interface FileUploadProps {
    onFileUpload: (file: File, passphrase?: string, ttlMillis?: number, oneTime?: boolean) => void;
    isUploading: boolean;
}

export default function FileUpload({ onFileUpload, isUploading }: FileUploadProps) {
    const [file, setFile] = useState<File | null>(null);
    const [passphrase, setPassphrase] = useState('');
    const [ttl, setTtl] = useState<number>(86400000); // default 24h
    const [oneTime, setOneTime] = useState(false);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!file) return;
        onFileUpload(file, passphrase, ttl, oneTime);
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-4">
            <div className="p-4 border-2 border-dashed rounded-lg text-center">
                <input
                    type="file"
                    onChange={(e) => setFile(e.target.files?.[0] || null)}
                    disabled={isUploading}
                    className="block w-full text-sm text-gray-600"
                />
                {file && <p className="mt-2 text-gray-700">{file.name}</p>}
            </div>

            <div>
                <label className="block text-sm font-medium mb-1">Passphrase (optional)</label>
                <input
                    type="text"
                    value={passphrase}
                    onChange={(e) => setPassphrase(e.target.value)}
                    placeholder="Enter a secret code"
                    className="input-field w-full"
                />
            </div>

            <div>
                <label className="block text-sm font-medium mb-1">Expiry</label>
                <select
                    value={ttl}
                    onChange={(e) => setTtl(Number(e.target.value))}
                    className="input-field w-full"
                >
                    <option value={3600000}>1 hour</option>
                    <option value={86400000}>24 hours</option>
                    <option value={604800000}>7 days</option>
                    <option value={0}>Never</option>
                </select>
            </div>

            <div className="flex items-center">
                <input
                    type="checkbox"
                    checked={oneTime}
                    onChange={(e) => setOneTime(e.target.checked)}
                    className="mr-2"
                />
                <label>One-time share (delete after first download)</label>
            </div>

            <button
                type="submit"
                className="btn-primary flex items-center justify-center w-full"
                disabled={isUploading || !file}
            >
                <FiUpload className="mr-2" />
                {isUploading ? 'Uploading...' : 'Upload File'}
            </button>
        </form>
    );
}
