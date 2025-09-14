import './globals.css';
import type { Metadata } from 'next';

export const metadata: Metadata = {
    title: 'P2PLink - Secure P2P File Sharing',
    description: 'Upload and share files securely with short invite codes.',
};

export default function RootLayout({
                                       children,
                                   }: {
    children: React.ReactNode;
}) {
    return (
        <html lang="en">
        <body>
        {children}
        </body>
        </html>
    );
}
