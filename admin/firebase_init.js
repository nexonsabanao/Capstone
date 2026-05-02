import { initializeApp } from "https://www.gstatic.com/firebasejs/12.8.0/firebase-app.js";
import { getFirestore } from "https://www.gstatic.com/firebasejs/12.8.0/firebase-firestore.js";
import { getAuth } from "https://www.gstatic.com/firebasejs/12.8.0/firebase-auth.js";

export const firebaseConfig = { 
    apiKey: "AIzaSyA7ML7csl8bwO_n1EWtYSscPlcZ7KS0gCY", 
    authDomain: "nutriority.firebaseapp.com", 
    projectId: "nutriority", 
    storageBucket: "nutriority.firebasestorage.app", 
    messagingSenderId: "427591895572", 
    appId: "1:427591895572:android:51325c2cba9e47b666f501" 
};

export const settings = {
    limitVal: 10
};

const app = initializeApp(firebaseConfig);
export const db = getFirestore(app);
export const auth = getAuth(app);
