import { db } from "./firebase_init.js";
import { collection, getDocs, writeBatch } from "https://www.gstatic.com/firebasejs/12.8.0/firebase-firestore.js";

// Helper to log to the sync console
export const log = (msg, type = '') => {
    const c = document.getElementById('syncConsole');
    if (!c) return;
    const entry = document.createElement('div');
    if (type === 'success') entry.style.color = '#10b981';
    if (type === 'err') entry.style.color = '#ef4444';
    entry.innerHTML = `<span class="timestamp">[${new Date().toLocaleTimeString()}]</span> ${msg}`;
    c.appendChild(entry);
    c.scrollTop = c.scrollHeight;
};

window.clearConsole = () => {
    const c = document.getElementById('syncConsole');
    if (c) c.innerHTML = "Console cleared...";
};

window.startExerciseSync = async () => { 
    log("🚀 Syncing Exercises..."); 
    // Logic goes here
    log("✅ Sync complete", "success");
};

window.startMealSync = async () => { 
    log("🍲 Syncing Meals..."); 
    // Logic goes here
    log("✅ Sync complete", "success");
};

window.startNewsSync = async () => { 
    log("📰 Syncing News..."); 
    // Logic goes here
    log("✅ Sync complete", "success");
};

window.confirmWipe = async (c) => { 
    if(confirm("WIPE all "+c+"?")) { 
        const s = await getDocs(collection(db,c)); 
        const b = writeBatch(db); 
        s.forEach(d=>b.delete(d.ref)); 
        await b.commit(); 
        log("✨ Collection wiped.", "success"); 
        if (window.updateStats) window.updateStats(); 
    } 
};
