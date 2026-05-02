import { db, auth, firebaseConfig } from "./firebase_init.js";
import { doc, setDoc, deleteDoc } from "https://www.gstatic.com/firebasejs/12.8.0/firebase-firestore.js";
import { createUserWithEmailAndPassword, sendPasswordResetEmail, getAuth, setPersistence, inMemoryPersistence } from "https://www.gstatic.com/firebasejs/12.8.0/firebase-auth.js";
import { initializeApp } from "https://www.gstatic.com/firebasejs/12.8.0/firebase-app.js";

window.closeModal = () => document.querySelectorAll('.modal').forEach(m=>m.style.display='none');

window.openStudentModal = (id=null) => {
    const form = document.getElementById('studentForm');
    if (!form) return;
    form.reset(); 
    document.getElementById('editStId').value = id || "";
    document.getElementById('stModalTitle').innerText = id ? "Edit User Profile" : "Add User Account";
    
    const submitBtn = document.getElementById('stSubmitBtn');
    if (submitBtn) submitBtn.innerText = id ? "Save Changes" : "Add User";

    const newPassGroup = document.getElementById('newAccountPass');
    const editPassGroup = document.getElementById('editAccountPass');
    const emailGroup = document.getElementById('stEmailGroup');
    const idGroup = document.getElementById('stIdGroup');
    const emailInput = document.getElementById('stEmail');

    if (id) {
        if (newPassGroup) newPassGroup.style.display = 'none';
        if (editPassGroup) editPassGroup.style.display = 'block';
        if (emailGroup) emailGroup.style.display = 'none';
        if (idGroup) idGroup.style.display = 'block';
        emailInput.removeAttribute('required');
        document.getElementById('stPass').removeAttribute('required');

        if(window.cacheSt) {
            const u = window.cacheSt.find(x => x.fid === id);
            if(u) {
                document.getElementById('stName').value = u.name || "";
                document.getElementById('stUID').value = u.fid || "";
                document.getElementById('stEmail').value = u.email || "";
                
                if (u.birthDate) {
                    const date = new Date(u.birthDate);
                    document.getElementById('stBirthDate').value = date.toISOString().split('T')[0];
                }

                document.getElementById('stGender').value = u.gender || "Male";
                document.getElementById('stGoal').value = u.goal || "Keep Fit";
                document.getElementById('stLevel').value = u.activityLevel || "Sedentary";
                document.getElementById('stDiet').value = u.preferredDiet || "Balanced";
                
                document.getElementById('stWeight').value = u.weightKg || 0;
                document.getElementById('stHeight').value = u.heightCm || 0;
                document.getElementById('stExclusions').value = (u.excludedIngredients || []).join(", ");
                document.getElementById('stWorkoutJson').value = u.personalizedPlanJson || "";
                document.getElementById('stMealJson').value = u.mealPlanJson || "";
            }
        }
    } else {
        if (newPassGroup) newPassGroup.style.display = 'block';
        if (editPassGroup) editPassGroup.style.display = 'none';
        if (emailGroup) emailGroup.style.display = 'block';
        if (idGroup) idGroup.style.display = 'none';
        emailInput.setAttribute('required', 'true');
        document.getElementById('stPass').setAttribute('required', 'true');
    }
    
    document.getElementById('studentModal').style.display='flex';
};

window.sendResetEmail = async () => {
    const email = document.getElementById('stEmail').value;
    if (!email) {
        alert("Email is required to send reset link.");
        return;
    }
    if (confirm(`Send a password reset link to ${email}?`)) {
        try {
            await sendPasswordResetEmail(auth, email);
            alert("Success! A password reset link has been sent to the user's inbox.");
        } catch (e) {
            alert("Error: " + e.message);
        }
    }
};

document.getElementById('studentForm').onsubmit = async (e) => {
    e.preventDefault(); 
    const fid = document.getElementById('editStId').value; 
    
    const birthDateInput = document.getElementById('stBirthDate').value;
    const birthDateTimestamp = birthDateInput ? new Date(birthDateInput).getTime() : null;

    const userData = { 
        name: document.getElementById('stName').value,
        gender: document.getElementById('stGender').value, 
        birthDate: birthDateTimestamp,
        goal: document.getElementById('stGoal').value, 
        activityLevel: document.getElementById('stLevel').value,
        weightKg: parseFloat(document.getElementById('stWeight').value) || 0, 
        heightCm: parseFloat(document.getElementById('stHeight').value) || 0, 
        preferredDiet: document.getElementById('stDiet').value,
        excludedIngredients: document.getElementById('stExclusions').value.split(',').map(i => i.trim()).filter(i => i !== ""),
        personalizedPlanJson: document.getElementById('stWorkoutJson').value,
        mealPlanJson: document.getElementById('stMealJson').value,
    };

    const submitBtn = document.getElementById('stSubmitBtn');
    const originalBtnText = submitBtn.innerText;
    submitBtn.innerText = "Processing...";
    submitBtn.disabled = true;

    try {
        if (!fid) {
            const email = document.getElementById('stEmail').value.toLowerCase().trim();
            const password = document.getElementById('stPass').value;

            const passRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;
            if (!passRegex.test(password)) {
                alert("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, and one number.");
                submitBtn.innerText = originalBtnText;
                submitBtn.disabled = false;
                return;
            }

            // Create a secondary app to avoid signing out the admin
            const secondaryApp = initializeApp(firebaseConfig, "Secondary");
            const secondaryAuth = getAuth(secondaryApp);
            
            try {
                // IMPORTANT: Set persistence to NONE (in-memory) for the secondary app
                // This prevents it from overwriting the admin's session in LocalStorage
                await setPersistence(secondaryAuth, inMemoryPersistence);
                
                const userCredential = await createUserWithEmailAndPassword(secondaryAuth, email, password);
                const uid = userCredential.user.uid;
                
                userData.id = uid;
                userData.email = email;
                userData.lastCompletedWorkoutDay = 0;
                userData.status = "active";
                userData.isEmailVerified = true;

                await setDoc(doc(db, "users", uid), userData);
                alert("Account added successfully!");
            } finally {
                // Always delete secondary app to clean up memory
                await secondaryApp.delete();
            }
        } else {
            const u = window.cacheSt.find(x => x.fid === fid);
            if (u && u.email) userData.email = u.email;
            
            await setDoc(doc(db, "users", fid), userData, { merge: true });
            alert("Profile updated!");
        }
        
        window.closeModal(); 
        if (window.updateStats) window.updateStats(); 
    } catch (error) {
        console.error("Error saving user:", error);
        alert("Error: " + error.message);
    } finally {
        submitBtn.innerText = originalBtnText;
        submitBtn.disabled = false;
    }
};

window.deleteRecord = async (coll, id, hardDelete = false) => {
    let msg = hardDelete ? "PERMANENTLY DELETE this user from Firestore?" : "Delete this record?";
    
    if (coll === 'users' && !hardDelete) {
        msg = "Are you sure? This will set user status to 'deleted' and force a logout on their app. \n\nNote: You must still manually delete them from 'Authentication' tab in Firebase Console to remove the email login.";
    }

    if(confirm(msg)) { 
        try {
            if (coll === 'users') {
                if (hardDelete) {
                    await deleteDoc(doc(db, coll, id));
                    alert("User document removed from Firestore.");
                } else {
                    const u = window.cacheSt.find(x => x.fid === id);
                    const updateData = { status: "deleted" };
                    if (u && u.email) updateData.email = u.email;
                    await setDoc(doc(db, coll, id), updateData, { merge: true });
                    alert("User access revoked.");
                }
            } else {
                await deleteDoc(doc(db, coll, id));
            }
            if (window.updateStats) window.updateStats(); 
        } catch (e) {
            alert("Operation failed: " + e.message);
        }
    }
};

window.openExModal = (id=null) => {
    const form = document.getElementById('exForm');
    if (!form) return;
    form.reset();
    document.getElementById('editExId').value = id || "";
    
    const submitBtn = document.getElementById('exSubmitBtn');
    if (submitBtn) submitBtn.innerText = id ? "Save Changes" : "Add Exercise";

    if(id && window.cacheEx) {
        const e = window.cacheEx.find(x => x.id === id);
        if(e) {
            document.getElementById('exName').value = e.name || "";
            document.getElementById('exGif').value = e.gifUrl || "";
            document.getElementById('exTarget').value = e.target || "";
            document.getElementById('exSecondary').value = e.secondary || "";
            document.getElementById('exCategory').value = e.category || "main exercise";
            document.getElementById('exDiff').value = e.difficulty || "Beginner";
            document.getElementById('exSteps').value = Array.isArray(e.instructions) ? e.instructions.join('\n') : (e.instructions || "");
        }
    }
    document.getElementById('exModal').style.display='flex';
};

window.openMealModal = (id=null) => {
    const form = document.getElementById('mealForm');
    if (!form) return;
    form.reset();
    document.getElementById('editMealId').value = id || "";

    const submitBtn = document.getElementById('mealSubmitBtn');
    if (submitBtn) submitBtn.innerText = id ? "Save Changes" : "Add Meals";

    if(id && window.cacheMl) {
        const m = window.cacheMl.find(x => x.id === id);
        if(m) {
            document.getElementById('mName').value = m.name || "";
            document.getElementById('mImg').value = m.imageName || "";
            document.getElementById('mDuration').value = m.duration || 0;
            document.getElementById('mTime').value = m.mealTime || "Breakfast";
            document.getElementById('mDiet').value = m.preferredDiet || "Balanced";
            document.getElementById('mCalories').value = m.calories || 0;
            document.getElementById('mP').value = m.macros?.protein || 0;
            document.getElementById('mC').value = m.macros?.carbs || 0;
            document.getElementById('mF').value = m.macros?.fats || 0;
            document.getElementById('mIng').value = Array.isArray(m.ingredients) ? m.ingredients.join(', ') : (m.ingredients || "");
            document.getElementById('mInst').value = m.instructions || "";
        }
    }
    document.getElementById('mealModal').style.display='flex';
};

window.openArtModal = (id=null) => {
    const form = document.getElementById('artForm');
    if (!form) return;
    form.reset();
    document.getElementById('editArtId').value = id || "";

    const submitBtn = document.getElementById('artSubmitBtn');
    if (submitBtn) submitBtn.innerText = id ? "Save Changes" : "Add Articles";

    if(id && window.cacheArt) {
        const a = window.cacheArt.find(x => x.id === id);
        if(a) {
            document.getElementById('aTitle').value = a.title || "";
            document.getElementById('aAuthor').value = a.author || "";
            document.getElementById('aUrl').value = a.articleUrl || "";
            document.getElementById('aSource').value = a.source || "";
            document.getElementById('aDate').value = a.date || "";
            document.getElementById('aImg').value = a.imageName || "";
            document.getElementById('aCat').value = a.category || "Wellness";
            document.getElementById('aDesc').value = a.description || "";
            document.getElementById('aContent').value = a.content || "";
        }
    }
    document.getElementById('artModal').style.display='flex';
};

document.getElementById('exForm').onsubmit = async (e) => { 
    e.preventDefault(); 
    const id = document.getElementById('editExId').value || ("ex_" + Date.now()); 
    await setDoc(doc(db, "exercises", id), { 
        id, 
        name: document.getElementById('exName').value, 
        gifUrl: document.getElementById('exGif').value, 
        target: document.getElementById('exTarget').value, 
        secondary: document.getElementById('exSecondary').value,
        category: document.getElementById('exCategory').value, 
        difficulty: document.getElementById('exDiff').value, 
        instructions: document.getElementById('exSteps').value.split('\n').filter(line => line.trim() !== "")
    }, {merge:true}); 
    window.closeModal(); 
    if (window.updateStats) window.updateStats(); 
};

document.getElementById('mealForm').onsubmit = async (e) => {
    e.preventDefault();
    const id = document.getElementById('editMealId').value || ("meal_" + Date.now());
    await setDoc(doc(db, "meals", id), {
        id,
        name: document.getElementById('mName').value,
        imageName: document.getElementById('mImg').value,
        duration: parseInt(document.getElementById('mDuration').value),
        mealTime: document.getElementById('mTime').value,
        preferredDiet: document.getElementById('mDiet').value,
        calories: parseInt(document.getElementById('mCalories').value),
        macros: {
            protein: parseInt(document.getElementById('mP').value),
            carbs: parseInt(document.getElementById('mC').value),
            fats: parseInt(document.getElementById('mF').value)
        },
        ingredients: document.getElementById('mIng').value.split(',').map(i => i.trim()).filter(i => i !== ""),
        instructions: document.getElementById('mInst').value
    }, {merge:true});
    window.closeModal(); 
    if (window.updateStats) window.updateStats(); 
};

document.getElementById('artForm').onsubmit = async (e) => {
    e.preventDefault();
    const id = document.getElementById('editArtId').value || ("art_" + Date.now());
    await setDoc(doc(db, "articles", id), {
        id,
        title: document.getElementById('aTitle').value,
        author: document.getElementById('aAuthor').value,
        articleUrl: document.getElementById('aUrl').value,
        source: document.getElementById('aSource').value,
        date: document.getElementById('aDate').value || new Date().toISOString(),
        imageName: document.getElementById('aImg').value,
        category: document.getElementById('aCat').value,
        description: document.getElementById('aDesc').value,
        content: document.getElementById('aContent').value
    }, {merge:true});
    window.closeModal(); 
    if (window.updateStats) window.updateStats(); 
};
