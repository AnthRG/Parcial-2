#!/usr/bin/env bash
echo "Instalando estructura basica para clase virtualhost y proxy reverso"

# Habilitando la memoria de intercambio.
sudo dd if=/dev/zero of=/swapfile count=2048 bs=1MiB
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
sudo cp /etc/fstab /etc/fstab.bak
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Instando los software necesarios para probar el concepto.
sudo apt update && sudo apt -y install apache2 certbot python3-certbot-apache unzip zip
sudo a2enmod ssl
sudo a2enmod proxy
sudo a2enmod proxy_http
sudo systemctl restart apache2

# Instalando la versión sdkman y java
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.3-tem

# Subiendo el servicio de Apache.
sudo service apache2 start

# Clonando el repositorio.
git clone https://github.com/vacax/virtualhost-proxyreverso

# Copiando los archivos de configuración en la ruta indicada.
sudo cp ~/virtualhost-proxyreverso/configuraciones/virtualhost.conf /etc/apache2/sites-available/
sudo cp ~/virtualhost-proxyreverso/configuraciones/seguro.conf /etc/apache2/sites-available/
sudo cp ~/virtualhost-proxyreverso/configuraciones/proxyreverso.conf /etc/apache2/sites-available/

# Creando las estructuras de los archivos.
sudo mkdir -p /var/www/html/app1 /var/www/html/app2

# Creando los archivos por defecto.
printf "<h1>Sitio Aplicacion #1</h1>" | sudo tee /var/www/html/app1/index.html
printf "<h1>Sitio Aplicacion #2</h1>" | sudo tee /var/www/html/app2/index.html

# Clonando el proyecto ORM y moviendo a la carpeta descargada.
cd ~/
git clone https://github.com/AnthRG/ApacheReady.git
cd ~/ApacheReady/proyecto-final

sudo bash -c 'cat > /etc/apache2/sites-available/seguro.conf << EOF
<VirtualHost *:80>
    ServerName suslinks.sus.codes
    Redirect permanent / https://suslinks.sus.codes
</VirtualHost>

<VirtualHost *:443>
    ServerName suslinks.sus.codes

    DocumentRoot /var/www/html

    SSLEngine on
    SSLCertificateFile /etc/letsencrypt/live/suslinks.sus.codes/fullchain.pem
    SSLCertificateKeyFile /etc/letsencrypt/live/suslinks.sus.codes/privkey.pem

    ProxyPass / http://localhost:7070/
    ProxyPassReverse / http://localhost:7070/

    <Directory "/var/www/html">
        AllowOverride All
        Require all granted
    </Directory>
</VirtualHost>
EOF'

sudo a2ensite seguro.conf
sudo systemctl reload apache2

DOMINIO="suslinks.sus.codes"
EMAIL="axrg0002@ce.pucmm.edu.do"

sudo systemctl stop apache2
if [ ! -f "/etc/letsencrypt/live/$DOMINIO/cert.pem" ]; then
    echo "Generando certificado SSL para $DOMINIO..."
    sudo certbot certonly --standalone -m $EMAIL -d $DOMINIO --agree-tos --non-interactive
else
    echo "Certificado para $DOMINIO ya existe."
fi
sudo systemctl start apache2
cd ~/ApacheReady/proyecto-final
chmod +x gradlew
nohup java -jar ~/ApacheReady/proyecto-final/build/libs/app.jar > ~/ApacheReady/proyecto-final/build/libs/salida.txt 2> ~/ApacheReady/proyecto-final/build/libs/error.txt &

# Ejecutando la creación de fatjar
./gradlew shadowjar

# Subiendo la aplicación puerto por defecto.
java -jar ~/Parcial-2/build/libs/app.jar > ~/Parcial-2/build/libs/salida.txt 2> ~/Parcial-2/build/libs/error.txt &
