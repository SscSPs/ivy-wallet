const fs = require("fs");
const path = require("path");

const inputFile = path.join(__dirname, "samplefile.json");
const outputFile = path.join(__dirname, "output.json");

// Load original file
// Read raw bytes
let buffer = fs.readFileSync(inputFile);

// Strip BOM for UTF-16BE (0xFE 0xFF)
if (buffer[0] === 0xFE && buffer[1] === 0xFF) {
  buffer = buffer.slice(2);
}

// Convert UTF-16BE → UTF-8 string manually
let raw = "";
for (let i = 0; i < buffer.length; i += 2) {
  const high = buffer[i];
  const low = buffer[i + 1];
  const codePoint = (high << 8) | low;
  raw += String.fromCharCode(codePoint);
}

const data = JSON.parse(raw);

// Lookup maps
const accountsById = new Map();
const categoriesById = new Map();

// Load existing accounts
if (Array.isArray(data.accounts)) {
  for (const acc of data.accounts) {
    accountsById.set(acc.id, acc);
  }
}

// Load categories
if (Array.isArray(data.categories)) {
  for (const cat of data.categories) {
    categoriesById.set(cat.id, cat);
  }
}

const categoryAccountUUID = new Map(); // key: categoryId-currency → UUID
const { randomUUID } = require("crypto");

// Cache for created category accounts
const createdCategoryAccounts = new Map();
const newCategoryAccountIds = new Set(); // <--- add this
const categoryStats = new Map(); // key: categoryId-currency → { expense, income }

for (const tx of data.transactions) {
  // only count usable transactions
  if ("dueDate" in tx) continue;
  if (!tx.categoryId) continue;
  if (tx.type !== "EXPENSE" && tx.type !== "INCOME") continue;

  const sourceAccount = accountsById.get(tx.accountId);
  if (!sourceAccount) continue;

  const key = `${tx.categoryId}-${sourceAccount.currency}`;

  if (!categoryStats.has(key)) {
    categoryStats.set(key, { expense: 0, income: 0 });
  }

  const stats = categoryStats.get(key);

  if (tx.type === "EXPENSE") stats.expense++;
  if (tx.type === "INCOME") stats.income++;
}

// Create or find category-account
function getOrCreateCategoryAccount(categoryId, currency) {
  const category = categoriesById.get(categoryId);
  if (!category) return null;

  // Unique cache key per category/currency
  const key = `${categoryId}-${currency}`;

  // If we already created, reuse
  if (createdCategoryAccounts.has(key)) {
    return createdCategoryAccounts.get(key);
  }

  // If we already generated a UUID before, use it
  let id;
  if (categoryAccountUUID.has(key)) {
    id = categoryAccountUUID.get(key);
  } else {
    id = randomUUID();
    categoryAccountUUID.set(key, id);
  }

  // decide accountCategory
  const stats = categoryStats.get(key) || { expense: 0, income: 0 };

  let accountCategory = "EXPENSE";
  if (stats.income > stats.expense) {
    accountCategory = "INCOME";
  } else if (stats.income === stats.expense) {
    // equal case -> print
    console.log(`⚠️ Equal expense/income for category: ${category.name} (${currency}), count: ${stats.expense}`);
  }


  const newAccount = {
    id: id,
    name: `${category.name} - ${currency}`,
    currency,
    color: category.color,
    icon: category.icon,
    isSynced: true,
    reconciliationDate: null,
    accountCategory // <-- NEW FIELD HERE
  };

  // Append to accounts and indexes
  data.accounts.push(newAccount);
  accountsById.set(id, newAccount);
  createdCategoryAccounts.set(key, newAccount);
  newCategoryAccountIds.add(id); // <--- mark this as a new category account

  return newAccount;
}

// Process transactions
const newTransactions = [];

for (const tx of data.transactions) {
  // Skip dueDate
  if ("dueDate" in tx) {
    newTransactions.push(tx);
    continue;
  }

  // Skip if category missing
  if (!tx.categoryId) {
    newTransactions.push(tx);
    continue;
  }

  // Lookup source account
  const sourceAccount = accountsById.get(tx.accountId);
  if (!sourceAccount) {
    newTransactions.push(tx);
    continue;
  }

  const currency = sourceAccount.currency;

  // Existing transfer → leave untouched
  if (tx.type === "TRANSFER") {
    newTransactions.push(tx);
    continue;
  }

  // Create matching category account
  const categoryAcc = getOrCreateCategoryAccount(tx.categoryId, currency);
  if (!categoryAcc) {
    newTransactions.push(tx);
    continue;
  }

  // Clone transaction (preserve all fields)
  const newTx = { ...tx };
  newTx.type = "TRANSFER";

  if (tx.type === "EXPENSE") {
    newTx.accountId = tx.accountId;
    newTx.toAccountId = categoryAcc.id;
    newTx.toAmount = tx.amount;
  }

  if (tx.type === "INCOME") {
    newTx.accountId = categoryAcc.id;
    newTx.toAccountId = tx.accountId;
    newTx.toAmount = tx.amount;
  }

  newTransactions.push(newTx);
}

data.transactions = newTransactions;

// ---- REORDER ACCOUNTS AND ASSIGN orderNum ----

// Put original accounts first (not in newCategoryAccountIds), then new category accounts,
// and within new category accounts group them by name, then by currency.
data.accounts.sort((a, b) => {
  const aNew = newCategoryAccountIds.has(a.id) ? 1 : 0;
  const bNew = newCategoryAccountIds.has(b.id) ? 1 : 0;

  // Originals first
  if (aNew !== bNew) return aNew - bNew;

  // Both new category accounts → group/sort by name, then currency
  if (aNew === 1 && bNew === 1) {
    const nameCmp = (a.name || "").localeCompare(b.name || "");
    if (nameCmp !== 0) return nameCmp;
    return (a.currency || "").localeCompare(b.currency || "");
  }

  // Both original accounts → keep existing relative order (Node's sort is stable)
  return 0;
});

// Now assign continuous orderNum
for (let i = 0; i < data.accounts.length; i++) {
  data.accounts[i].orderNum = i + 1;
}

// Convert JS string -> UTF-16BE bytes
function encodeUTF16BE(str, addBOM = true) {
  const buf = Buffer.alloc((str.length * 2) + (addBOM ? 2 : 0));

  let offset = 0;

  // Write BOM if requested
  if (addBOM) {
    buf[offset++] = 0xFE;
    buf[offset++] = 0xFF;
  }

  // Encode each char
  for (let i = 0; i < str.length; i++) {
    const code = str.charCodeAt(i);
    buf[offset++] = (code >> 8) & 0xFF; // high byte
    buf[offset++] = code & 0xFF;        // low byte
  }

  return buf;
}

// Convert new JSON to string
const outputString = JSON.stringify(data);

// Encode to UTF-16BE with BOM
const outputBuffer = encodeUTF16BE(outputString, true);

// Write to file
fs.writeFileSync(outputFile, outputBuffer);

console.log("Output written as UTF-16 BE:", outputFile);

const { execSync } = require("child_process");

try {
  // remove existing zip
  if (fs.existsSync("output.zip")) {
    fs.unlinkSync("output.zip");
  }

  // create zip using system zip command
  execSync(`zip -q -9 output.zip output.json`);

  console.log("Created output.zip (deflate)");
} catch (err) {
  console.error("Failed to create zip:", err);
}