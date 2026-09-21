FROM nodered/node-red:2.2.2-16
USER root
RUN apk add --no-cache wget unzip
LABEL org.opencontainers.image.title="Snap4City Node-RED ARM" \
      org.opencontainers.image.description="Port ARM64 della palette Snap4City Node-RED 2.2.2 ADV v14 con SSO" \
      org.opencontainers.image.base.reference="disitlab/snap4city-nodered-v2.2.2-adv:v14" \
      org.opencontainers.image.base.architecture="amd64-reference-rebuilt-for-arm64"
WORKDIR /opt/snap4city
ENV NODE_ENV=production \
    NR_RUNTIME_DIR=/usr/src/node-red \
    NODE_PATH=/opt/snap4city/node_modules:/usr/src/node-red/node_modules:/data/node_modules
# Solo estensioni: Node-RED resta quello gia installato nell'immagine ufficiale.
COPY docker/package.json docker/package-lock.json ./
RUN npm ci --omit=optional --ignore-scripts --no-audit --no-fund && npm cache clean --force
COPY upstream ./upstream
COPY scripts ./scripts
COPY config ./config
RUN node scripts/prepare-runtime.js \
    && mkdir -p /data \
    && chown node-red:root /data \
    && chmod 700 /data
WORKDIR /data
USER node-red
EXPOSE 1880
VOLUME ["/data"]
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s CMD node /opt/snap4city/scripts/healthcheck.js
# Sostituisce l'entrypoint npm della base, che dipende dal suo package.json.
ENTRYPOINT ["node", "/usr/src/node-red/node_modules/node-red/red.js"]
CMD ["--userDir", "/data", "--settings", "/opt/snap4city/config/settings.js"]
