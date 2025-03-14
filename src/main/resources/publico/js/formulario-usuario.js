// js/formulario-usuario.js

window.addEventListener('load', () => {
    setTimeout(() => {
        const formulario = document.getElementById("formulario");
        formulario.classList.remove("opacity-0", "translate-y-10");
        formulario.classList.add("opacity-100", "translate-y-0");
    }, 100);
    cargarRegistroPendienteUsuario();
});

document.getElementById('saveUser').addEventListener('click', function () {
    const userData = {
        nombre: document.querySelector('[name="nombre"]').value,
        username: document.querySelector('[name="username"]').value,
        password: document.querySelector('[name="password"]').value,
        rol: document.querySelector('[name="rol"]').value
    };
    localforage.getItem('pendingUsers').then(existing => {
        let users = existing || [];
        const params = new URLSearchParams(window.location.search);
        const editIndex = params.get('editPendingUser');
        if (editIndex !== null) {
            // Actualiza el registro pendiente existente
            users[editIndex] = userData;
        } else {
            // Sino, se agrega un nuevo registro pendiente
            users.push(userData);
        }
        return localforage.setItem('pendingUsers', users);
    }).then(() => {
        alert("Datos guardados offline. Se sincronizarán desde el dashboard.");
        window.location.href = "/dashboard";
    });
});

function cargarRegistroPendienteUsuario() {
    const params = new URLSearchParams(window.location.search);
    const editIndex = params.get('editPendingUser');
    if (editIndex !== null) {
        localforage.getItem('pendingUsers').then(users => {
            users = users || [];
            const registro = users[editIndex];
            if (registro) {
                document.querySelector('[name="nombre"]').value = registro.nombre;
                document.querySelector('[name="username"]').value = registro.username;
                document.querySelector('[name="password"]').value = registro.password;
                document.querySelector('[name="rol"]').value = registro.rol;
            }
        });
    }
}
