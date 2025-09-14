/** @type {import('next').NextConfig} */
const nextConfig = {
    reactStrictMode: true,

    eslint: {
        ignoreDuringBuilds: true,
    },
    typescript: {
        ignoreBuildErrors: true,
    },

    async rewrites() {

            // In production → let Nginx handle /api
            return [];
    },
};

module.exports = nextConfig;


/* async rewrites() {
     return [
         {
             source: '/api/upload',
             destination: 'http://backend:8080/upload', // ✅ works in Docker Compose
         },
         {
             source: '/api/download/:fileId',
             destination: 'http://backend:8080/download/:fileId',
         },
         {
             source: '/api/events/:fileId',
             destination: 'http://backend:8080/events/:fileId',
         },
     ];
 },*/



