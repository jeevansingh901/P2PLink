'use client';

import { useState } from 'react';
import FileUpload from '@/components/FileUpload';
import FileDownload from '@/components/FileDownload';
import InviteCode from '@/components/InviteCode';
import axios from 'axios';

export default function Home() {
    const [uploadedFile, setUploadedFile] = useState<File | null>(null);
    const [isUploading, setIsUploading] = useState(false);
    const [isDownloading, setIsDownloading] = useState(false);
    const [fileId, setFileId] = useState<string | null>(null);
    const [progress, setProgress] = useState<number>(0);
    const [activeTab, setActiveTab] = useState<'upload' | 'download'>('upload');

    const handleFileUpload = async (
        file: File,
        passphrase?: string,
        ttlMillis?: number,
        oneTime?: boolean
    ) => {
        setUploadedFile(file);
        setIsUploading(true);
        setProgress(0);

        try {
            const CHUNK_SIZE = 5 * 1024 * 1024; // 5 MB
            const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
            let code: string | null = null;

            for (let i = 0; i < file.size; i += CHUNK_SIZE) {
                const chunk = file.slice(i, i + CHUNK_SIZE);
                const chunkIndex = Math.floor(i / CHUNK_SIZE);

                const headers: Record<string, string> = {
                    'Content-Type': 'application/octet-stream',
                    'X-File-Name': file.name,
                    'X-Chunk-Index': chunkIndex.toString(),
                    'X-Total-Chunks': totalChunks.toString(),
                    'X-File-Size': file.size.toString(),
                };
                if (passphrase) headers['X-Passphrase'] = passphrase;
                if (ttlMillis) headers['X-TTL-Millis'] = ttlMillis.toString();
                if (oneTime) headers['X-One-Time'] = 'true';

                const res = await axios.post('/api/upload', chunk, { headers });

                // Update progress locally
                const uploaded = Math.min(i + CHUNK_SIZE, file.size);
                setProgress((uploaded / file.size) * 100);

                if (chunkIndex === totalChunks - 1) {
                    code = res.data.fileId;
                }
            }

            if (code) {
                setFileId(code);
            }
        } catch (error) {
            console.error('Error uploading file:', error);
            alert('Failed to upload file. Please try again.');
        } finally {
            setIsUploading(false);
        }
    };

    const handleDownload = async (code: string, passphrase?: string) => {
        setIsDownloading(true);

        try {
            const response = await fetch(`/api/download/${code}`, {
                headers: passphrase ? { 'X-Passphrase': passphrase } : {},
            });

            if (!response.ok) {
                if (response.status === 401) {
                    throw new Error('Passphrase required or invalid');
                }
                throw new Error(`Failed to download file: ${response.statusText}`);
            }

            const disposition = response.headers.get('content-disposition');
            let filename = 'downloaded-file';
            if (disposition) {
                const match = disposition.match(/filename=\"(.+)\"/);
                if (match && match[1]) filename = match[1];
            }

            const blob = await response.blob();
            const objectUrl = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = objectUrl;
            link.download = filename;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(objectUrl);
        } catch (error: any) {
            console.error('Error downloading file:', error);
            alert(error.message || 'Download failed');
        } finally {
            setIsDownloading(false);
        }
    };

    return (
        <div className="container mx-auto px-4 py-8 max-w-4xl">
            <header className="text-center mb-12">
                <h1 className="text-4xl font-bold text-blue-600 mb-2">P2PLink</h1>
                <p className="text-xl text-gray-600">Secure P2P File Sharing</p>
            </header>

            <div className="bg-white rounded-lg shadow-lg p-6">
                <div className="flex border-b mb-6">
                    <button
                        className={`px-4 py-2 font-medium ${
                            activeTab === 'upload'
                                ? 'text-blue-600 border-b-2 border-blue-600'
                                : 'text-gray-500 hover:text-gray-700'
                        }`}
                        onClick={() => setActiveTab('upload')}
                    >
                        Share a File
                    </button>
                    <button
                        className={`px-4 py-2 font-medium ${
                            activeTab === 'download'
                                ? 'text-blue-600 border-b-2 border-blue-600'
                                : 'text-gray-500 hover:text-gray-700'
                        }`}
                        onClick={() => setActiveTab('download')}
                    >
                        Receive a File
                    </button>
                </div>

                {activeTab === 'upload' ? (
                    <div>
                        <FileUpload onFileUpload={handleFileUpload} isUploading={isUploading} />

                        {uploadedFile && !isUploading && (
                            <div className="mt-4 p-3 bg-gray-50 rounded-md">
                                <p className="text-sm text-gray-600">
                                    Selected file:{' '}
                                    <span className="font-medium">{uploadedFile.name}</span> (
                                    {Math.round(uploadedFile.size / 1024)} KB)
                                </p>
                            </div>
                        )}

                        {isUploading && (
                            <div className="mt-6 text-center">
                                <div className="w-full bg-gray-200 rounded-full h-4">
                                    <div
                                        className="bg-blue-500 h-4 rounded-full transition-all"
                                        style={{ width: `${progress}%` }}
                                    ></div>
                                </div>
                                <p className="mt-2 text-gray-600">
                                    Uploading... {progress.toFixed(2)}%
                                </p>
                            </div>
                        )}

                        <InviteCode fileId={fileId} />
                    </div>
                ) : (
                    <div>
                        <FileDownload onDownload={handleDownload} isDownloading={isDownloading} />
                    </div>
                )}
            </div>

            <footer className="mt-12 text-center text-gray-500 text-sm">
                <p>P2PLink &copy; {new Date().getFullYear()} - Secure P2P File Sharing</p>
            </footer>
        </div>
    );
}
