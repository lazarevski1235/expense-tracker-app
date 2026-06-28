const params = new URLSearchParams(window.location.search);
const view = params.get("view") === "weekly" ? "weekly" : "monthly";

document.getElementById(view === "weekly" ? "weeklyLink" : "monthlyLink").classList.add("active");
document.getElementById("incomeLabel").textContent = view === "weekly" ? "Income This Week" : "Income This Month";
document.getElementById("expenseLabel").textContent = view === "weekly" ? "Expenses This Week" : "Expenses This Month";
document.getElementById("balanceLabel").textContent = view === "weekly" ? "Balance This Week" : "Balance This Month";

function money(n) {
    return "$" + Number(n).toFixed(2);
}

fetch("/api/summary?view=" + view)
    .then(res => res.json())
    .then(summary => {
        document.getElementById("incomeValue").textContent = money(summary.totalIncome);
        document.getElementById("expenseValue").textContent = money(summary.totalExpense);
        document.getElementById("balanceValue").textContent = money(summary.balance);
    });

fetch("/api/charts?view=" + view)
    .then(res => res.json())
    .then(chart => {
        document.getElementById("chartTitle").textContent = chart.title;

        new Chart(document.getElementById("barChart"), {
            type: "bar",
            data: {
                labels: chart.labels,
                datasets: [
                    { label: "Income", data: chart.income, backgroundColor: "green" },
                    { label: "Expenses", data: chart.expense, backgroundColor: "red" }
                ]
            },
            options: {
                responsive: true,
                scales: { y: { beginAtZero: true } }
            }
        });
    });

fetch("/api/categories")
    .then(res => res.json())
    .then(cat => {
        if (cat.labels.length === 0) {
            document.getElementById("doughnutChart").style.display = "none";
            document.getElementById("doughnutEmpty").style.display = "block";
            return;
        }
        new Chart(document.getElementById("doughnutChart"), {
            type: "doughnut",
            data: {
                labels: cat.labels,
                datasets: [{ data: cat.amounts }]
            },
            options: { responsive: true }
        });
    });

fetch("/api/transactions")
    .then(res => res.json())
    .then(transactions => {
        const recent = transactions.slice(0, 5);
        const body = document.getElementById("recentBody");

        if (recent.length === 0) {
            document.getElementById("recentTable").style.display = "none";
            document.getElementById("recentEmpty").style.display = "block";
            return;
        }

        recent.forEach(t => {
            const row = document.createElement("tr");
            row.innerHTML =
                "<td>" + t.date + "</td>" +
                "<td>" + t.type + "</td>" +
                "<td>" + t.category + "</td>" +
                "<td>" + (t.description || "-") + "</td>" +
                "<td>" + money(t.amount) + "</td>";
            body.appendChild(row);
        });
    });
