# Usa l'immagine base ufficiale di PHP con Apache
FROM php:8.2-apache

# Installa le estensioni necessarie: ldap, pdo_mysql e composer
RUN apt-get update && apt-get install -y \
        libldap2-dev \
        libssl-dev \
        unzip \
        git \
        && rm -rf /var/lib/apt/lists/* \
    && docker-php-ext-configure ldap --with-ldap=/usr \
    && docker-php-ext-install ldap pdo pdo_mysql

# Installa Composer globalmente
COPY --from=composer:2 /usr/bin/composer /usr/bin/composer

# Copia il codice dell'app
COPY src/ /var/www/html/

# Installa PHPMailer tramite Composer
WORKDIR /var/www/html
RUN composer require phpmailer/phpmailer

# Espone la porta 80
EXPOSE 80

# Imposta i permessi per Apache
RUN chown -R www-data:www-data /var/www/html
