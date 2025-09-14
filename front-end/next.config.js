/** @type {import('next').NextConfig} */
const nextConfig = {
    reactStrictMode: true,
    // ❌ swcMinify removed in Next 13+, always true
    // ❌ experimental.turbo removed, replaced by turbopack
    turbopack: {}, // enables Turbopack (optional)
    async rewrites() {
        return [
            {
                source: '/api/upload',
                destination: 'http://localhost:8080/upload',
            },
            {
                source: '/api/download/:fileId',
                destination: 'http://localhost:8080/download/:fileId',
            },
            {
                source: '/api/events/:fileId',
                destination: 'http://localhost:8080/events/:fileId',
            },
        ];
    },
};

module.exports = nextConfig;
