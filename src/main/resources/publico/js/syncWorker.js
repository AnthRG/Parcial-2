// syncWorker.js

// Escucha el evento que indica que la conexión se ha restablecido
window.addEventListener('online', function() {
    console.log("Conexión restablecida. Intentando sincronizar datos...");

    // Recupera los datos guardados (si los hay)
    let storedData = JSON.parse(localStorage.getItem('offlineData')) || [];
    if (storedData.length > 0) {
        storedData.forEach(data => {
            // Construir el cuerpo de la petición en formato URL encoded
            let params = new URLSearchParams();
            params.append("nombre", data.nombre);
            params.append("sector", data.sector);
            params.append("nivelEscolar", data.nivelEscolar);
            params.append("latitud", data.latitud);
            params.append("longitud", data.longitud);

            // Realiza la petición al servidor (ajusta la URL según corresponda)
            fetch('/procesar-estudiante', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: params.toString()
            })
                .then(response => {
                    if (response.ok) {
                        console.log("Datos sincronizados correctamente:", data);
                    } else {
                        console.error("Error al sincronizar datos:", data);
                    }
                })
                .catch(error => console.error("Error en la sincronización:", error));
        });
        // Limpia los datos una vez intentada la sincronización
        localStorage.removeItem('offlineData');
    }
});
