import { auth, db } from "./firebase_init.js";
import { signInWithEmailAndPassword, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/12.8.0/firebase-auth.js";
import { doc, getDoc } from "https://www.gstatic.com/firebasejs/12.8.0/firebase-firestore.js";

const loginForm = document.getElementById('loginForm');
const emailInput = document.getElementById('email');
const passwordInput = document.getElementById('password');
const errorMessage = document.getElementById('errorMessage');
const loginBtn = document.getElementById('loginBtn');
const btnText = document.getElementById('btnText');
const btnLoader = document.getElementById('btnLoader');
const btnIcon = document.getElementById('btnIcon');

// Check for URL parameters (e.g., unauthorized access)
const urlParams = new URLSearchParams(window.location.search);
if (urlParams.get('error') === 'unauthorized') {
    showError("Access Denied: You do not have administrator privileges.");
}

// Check if already logged in
onAuthStateChanged(auth, async (user) => {
    if (user) {
        const isAdmin = await verifyAdmin(user.uid);
        if (isAdmin) {
            window.location.href = 'admin_dashboard.html';
        }
    }
});

async function verifyAdmin(uid) {
    try {
        const adminDoc = await getDoc(doc(db, "admins", uid));
        return adminDoc.exists();
    } catch (error) {
        console.error("Error verifying admin:", error);
        return false;
    }
}

loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const email = emailInput.value;
    const password = passwordInput.value;
    
    // UI Loading State
    errorMessage.style.display = 'none';
    btnText.style.display = 'none';
    if (btnIcon) btnIcon.style.display = 'none';
    btnLoader.style.display = 'block';
    loginBtn.disabled = true;

    try {
        const userCredential = await signInWithEmailAndPassword(auth, email, password);
        const user = userCredential.user;
        
        // Verify if user is an admin in Firestore
        const isAdmin = await verifyAdmin(user.uid);
        
        if (isAdmin) {
            window.location.href = 'admin_dashboard.html';
        } else {
            // Not an admin, sign them out
            await auth.signOut();
            showError("Access Denied: You do not have administrator privileges.");
        }
    } catch (error) {
        console.error("Login error:", error);
        let message = "An error occurred during login.";
        if (error.code === 'auth/user-not-found' || error.code === 'auth/wrong-password' || error.code === 'auth/invalid-credential') {
            message = "Invalid email or password.";
        } else if (error.code === 'auth/too-many-requests') {
            message = "Too many failed attempts. Please try again later.";
        }
        showError(message);
    } finally {
        btnText.style.display = 'block';
        if (btnIcon) btnIcon.style.display = 'block';
        btnLoader.style.display = 'none';
        loginBtn.disabled = false;
    }
});

function showError(msg) {
    errorMessage.innerText = msg;
    errorMessage.style.display = 'block';
}
