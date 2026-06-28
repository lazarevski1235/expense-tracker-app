const params = new URLSearchParams(window.location.search);
const from = params.get("from");
const to = params.get("to");

if (from) document.getElementById("fromInput").value = from;
if (to) document.getElementById("toInput").value = to;

function money(n) {
    return "$" + Number(n).toFixed(2);
}

let url = "/api/transactions";
if (from && to) {
    url += "?from=" + from + "&to=" + to;
}

fetch(url)
    .then(res => res.json())
    .then(transactions => {
        const body = document.getElementById("txBody");

        if (transactions.length === 0) {
            document.getElementById("txTable").style.display = "none";
            document.getElementById("txEmpty").style.display = "block";
            return;
        }

        transactions.forEach(t => {
            const row = document.createElement("tr");
            row.innerHTML =
                "<td>" + t.date + "</td>" +
                "<td>" + t.type + "</td>" +
                "<td>" + t.category + "</td>" +
                "<td>" + (t.description || "-") + "</td>" +
                "<td>" + money(t.amount) + "</td>" +
                "<td>" +
                "<a href='transaction-form.html?id=" + t.id + "'>Edit</a> " +
                "<button data-id='" + t.id + "' class='deleteBtn'>Delete</button>" +
                "</td>";
            body.appendChild(row);
        });

        document.querySelectorAll(".deleteBtn").forEach(btn => {
            btn.addEventListener("click", () => {
                if (!confirm("Delete this transaction?")) return;
                fetch("/api/transactions/" + btn.dataset.id, { method: "DELETE" })
                    .then(() => window.location.reload());
            });
        });
    });
