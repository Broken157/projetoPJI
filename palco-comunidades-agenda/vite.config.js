import {defineConfig} from 'vite';
export default defineConfig({
  server: {host:'localhost', port:3000, proxy:{
    '/api':{target:'http://localhost:8080',changeOrigin:false},
    '/ws':{target:'ws://localhost:8080',ws:true,changeOrigin:false}
  }},
  preview:{host:'localhost',port:3000},
});

