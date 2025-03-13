// offlineSync.js

// Variable para almacenar datos temporalmente (además de localStorage para persistencia)
let offlineData = [];

// Al cargar el DOM, se agrega el listener al formulario
document.addEventListener('DOMContentLoaded', () => {
    const form = document.querySelector('form');
    if (form) {
        form.addEventListener('submit', function(e) {
            // Si no hay conexión, se evita el envío normal
            if (!navigator.onLine) {
                e.preventDefault();

                // Capturamos los datos del formulario
                const formData = {
                    nombre: document.getElementById('nombre').value,
                    sector: document.getElementById('sector').value,
                    nivelEscolar: document.getElementById('nivelEscolar').value,
                    latitud: document.getElementById('latitud').value,
                    longitud: document.getElementById('longitud').value
                };

                // Guardamos en la variable y en localStorage para persistencia
                offlineData.push(formData);
                localStorage.setItem('offlineData', JSON.stringify(offlineData));

                alert("No hay conexión. Los datos se han guardado localmente y se sincronizarán cuando vuelvas a estar en línea.");
            }
            // Si está en línea, el envío se procesa normalmente.
        });
    }
});

// Opcional: Escuchar cuando se pierde la conexión (solo para depuración)
window.addEventListener('offline', () => {
    console.log("Has perdido la conexión a Internet.");
});
