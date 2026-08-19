# Copy the files from the dist to the container
FROM nginx:stable
COPY dist/app/browser /usr/share/nginx/html

# Copy the config
COPY nginx.conf /etc/nginx/nginx.conf