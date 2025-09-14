/** @type {import('next').NextConfig} */
const isProd = process.env.NODE_ENV === "production";

const nextConfig = {
    reactStrictMode: true,

    eslint: {
        ignoreDuringBuilds: true,
    },
    typescript: {
        ignoreBuildErrors: true,
    },

    async rewrites() {
        if (isProd) {
            // In production → let Nginx handle /api
            return [];
        } else {
            // In dev → forward /api to local backend
            return [
                { source: "/api/:path*", destination: "http://localhost:8080/:path*" }
            ];
        }
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



