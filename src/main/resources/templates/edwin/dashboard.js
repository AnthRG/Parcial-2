// js/dashboard.js

const openSidebarBtn = document.getElementById('openSidebar');
const closeSidebarBtn = document.getElementById('closeSidebar');
const overlayMenu = document.getElementById('overlayMenu');

// Abrir y cerrar el overlay del menú
openSidebarBtn.addEventListener('click', () => {
    overlayMenu.classList.remove('-translate-x-full');
    overlayMenu.classList.add('translate-x-0');
});
closeSidebarBtn.addEventListener('click', () => {
    overlayMenu.classList.remove('translate-x-0');
    overlayMenu.classList.add('-translate-x-full');
});

function loadPendingRecordsDashboard() {
    // Estudiantes pendientes
    localforage.getItem('surveyData').then(students => {
        students = students || [];
        const list = document.getElementById('pendingStudentsList');
        list.innerHTML = '';
        students.forEach((student, index) => {
            const li = document.createElement('li');
            li.className = 'p-4 border-b';
            li.innerHTML = `
        <div>
          <strong>Nombre:</strong> ${student.nombre}<br>
          <strong>Sector:</strong> ${student.sector}<br>
          <strong>Nivel:</strong> ${student.nivelEscolar}
        </div>
        <div class="mt-2">
          <button onclick="sendPendingStudent(${index})" class="bg-green-500 text-white px-2 py-1 rounded">Enviar</button>
          <button onclick="editPendingStudent(${index})" class="bg-yellow-500 text-white px-2 py-1 rounded">Editar</button>
          <button onclick="deletePendingStudent(${index})" class="bg-red-500 text-white px-2 py-1 rounded">Eliminar</button>
        </div>`;
            list.appendChild(li);
        });
    });
    // Usuarios pendientes
    localforage.getItem('pendingUsers').then(users => {
        users = users || [];
        const list = document.getElementById('pendingUsersList');
        list.innerHTML = '';
        users.forEach((user, index) => {
            const li = document.createElement('li');
            li.className = 'p-4 border-b';
            li.innerHTML = `
        <div>
          <strong>Usuario:</strong> ${user.username}<br>
          <strong>Nombre:</strong> ${user.nombre}<br>
          <strong>Rol:</strong> ${user.rol}
        </div>
        <div class="mt-2">
          <button onclick="sendPendingUser(${index})" class="bg-green-500 text-white px-2 py-1 rounded">Enviar</button>
          <button onclick="editPendingUser(${index})" class="bg-yellow-500 text-white px-2 py-1 rounded">Editar</button>
          <button onclick="deletePendingUser(${index})" class="bg-red-500 text-white px-2 py-1 rounded">Eliminar</button>
        </div>`;
            list.appendChild(li);
        });
    });
}

function sendPendingStudent(index) {
    localforage.getItem('surveyData').then(records => {
        records = records || [];
        const student = records[index];
        return fetch('/estudiantes/crear', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: new URLSearchParams(student)
        }).then(response => {
            if (response.ok) {
                records.splice(index, 1);
                return localforage.setItem('surveyData', records);
            } else {
                alert("Error al enviar registro: " + response.statusText);
            }
        });
    }).then(() => window.location.reload());
}

function editPendingStudent(index) {
    window.location.href = "/estudiantes/crear?editPending=" + index;
}

function deletePendingStudent(index) {
    localforage.getItem('surveyData').then(records => {
        records = records || [];
        records.splice(index, 1);
        return localforage.setItem('surveyData', records);
    }).then(() => loadPendingRecordsDashboard());
}

function sendPendingUser(index) {
    localforage.getItem('pendingUsers').then(records => {
        records = records || [];
        const user = records[index];
        return fetch('/register', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                username: user.username,
                nombre: user.nombre,
                password: user.password,
                administrador: user.rol === 'admin'
            })
        }).then(response => {
            if (response.ok) {
                records.splice(index, 1);
                return localforage.setItem('pendingUsers', records);
            } else {
                alert("Error al enviar registro: " + response.statusText);
            }
        });
    }).then(() => window.location.reload());
}

function editPendingUser(index) {
    window.location.href = "/crear-usuario?editPendingUser=" + index;
}

function deletePendingUser(index) {
    localforage.getItem('pendingUsers').then(records => {
        records = records || [];
        records.splice(index, 1);
        return localforage.setItem('pendingUsers', records);
    }).then(() => loadPendingRecordsDashboard());
}

window.addEventListener('load', loadPendingRecordsDashboard);
