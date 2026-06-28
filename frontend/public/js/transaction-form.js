const params = new URLSearchParams(window.location.search);
const id = params.get("id");

if (id) {
    document.getElementById("pageTitle").textContent = "Edit Transaction - Expense Tracker";
    document.getElementById("formTitle").textContent = "Edit Transaction";
    document.getElementById("submitBtn").textContent = "Save Changes";

    fetch("/api/transactions/" + id)
        .then(res => res.json())
        .then(t => {
            document.getElementById("type").value = t.type;
            document.getElementById("category").value = t.category;
            document.getElementById("amount").value = t.amount;
            document.getElementById("date").value = t.date;
            document.getElementById("description").value = t.description || "";
        });
}

document.getElementById("transactionForm").addEventListener("submit", function (e) {
    e.preventDefault();

    const body = {
        type: document.getElementById("type").value,
        category: document.getElementById("category").value,
        amount: document.getElementById("amount").value,
        date: document.getElementById("date").value,
        description: document.getElementById("description").value
    };

    const url = id ? "/api/transactions/" + id : "/api/transactions";
    const method = id ? "PUT" : "POST";

    fetch(url, {
        method: method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    }).then(res => {
        if (res.ok) {
            window.location.href = "transactions.html";
            return;
        }
        res.json().then(errors => {
            const box = document.getElementById("errorBox");
            box.style.display = "block";
            box.textContent = Object.values(errors).join(", ");
        });
    });
});
