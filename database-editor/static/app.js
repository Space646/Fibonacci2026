// Client-side only storage using localStorage
const STORAGE_KEY = 'foods_db_v1';

const initialFoods = [
  { id: 1, name: 'Apple', calories_per_100g: 52, protein_per_100g: 0.3, fat_per_100g: 0.2, sugar_per_100g: 10.0, fiber_per_100g: 2.4, is_healthy: 1, huskylens_label_id: 1 },
  { id: 2, name: 'Banana', calories_per_100g: 89, protein_per_100g: 1.1, fat_per_100g: 0.3, sugar_per_100g: 12.2, fiber_per_100g: 2.6, is_healthy: 1, huskylens_label_id: 2 },
  { id: 3, name: 'Orange', calories_per_100g: 47, protein_per_100g: 0.9, fat_per_100g: 0.1, sugar_per_100g: 9.4, fiber_per_100g: 2.4, is_healthy: 1, huskylens_label_id: 3 },
  { id: 4, name: 'Broccoli', calories_per_100g: 34, protein_per_100g: 2.8, fat_per_100g: 0.4, sugar_per_100g: 1.7, fiber_per_100g: 2.6, is_healthy: 1, huskylens_label_id: 4 },
  { id: 5, name: 'Carrot', calories_per_100g: 41, protein_per_100g: 0.9, fat_per_100g: 0.2, sugar_per_100g: 4.7, fiber_per_100g: 2.8, is_healthy: 1, huskylens_label_id: 5 },
  { id: 6, name: 'Chicken Breast', calories_per_100g: 165, protein_per_100g: 31.0, fat_per_100g: 3.6, sugar_per_100g: 0.0, fiber_per_100g: 0.0, is_healthy: 1, huskylens_label_id: 6 },
  { id: 7, name: 'Egg', calories_per_100g: 155, protein_per_100g: 13.0, fat_per_100g: 11.0, sugar_per_100g: 0.6, fiber_per_100g: 0.0, is_healthy: 1, huskylens_label_id: 7 },
  { id: 8, name: 'White Rice', calories_per_100g: 130, protein_per_100g: 2.7, fat_per_100g: 0.3, sugar_per_100g: 0.0, fiber_per_100g: 0.4, is_healthy: 0, huskylens_label_id: 8 },
  { id: 9, name: 'Whole Wheat Bread', calories_per_100g: 247, protein_per_100g: 13.0, fat_per_100g: 3.5, sugar_per_100g: 5.0, fiber_per_100g: 6.0, is_healthy: 1, huskylens_label_id: 9 },
  { id: 10, name: 'Cheddar Cheese', calories_per_100g: 402, protein_per_100g: 25.0, fat_per_100g: 33.0, sugar_per_100g: 0.1, fiber_per_100g: 0.0, is_healthy: 0, huskylens_label_id: 10 },
  { id: 11, name: 'Chocolate Bar', calories_per_100g: 535, protein_per_100g: 4.9, fat_per_100g: 29.7, sugar_per_100g: 56.9, fiber_per_100g: 3.4, is_healthy: 0, huskylens_label_id: 11 },
  { id: 12, name: 'Potato Chips', calories_per_100g: 547, protein_per_100g: 6.5, fat_per_100g: 37.0, sugar_per_100g: 0.4, fiber_per_100g: 4.4, is_healthy: 0, huskylens_label_id: 12 },
  { id: 13, name: 'Salmon', calories_per_100g: 208, protein_per_100g: 20.0, fat_per_100g: 13.0, sugar_per_100g: 0.0, fiber_per_100g: 0.0, is_healthy: 1, huskylens_label_id: 13 },
  { id: 14, name: 'Greek Yogurt', calories_per_100g: 59, protein_per_100g: 10.0, fat_per_100g: 0.4, sugar_per_100g: 3.2, fiber_per_100g: 0.0, is_healthy: 1, huskylens_label_id: 14 },
  { id: 15, name: 'Avocado', calories_per_100g: 160, protein_per_100g: 2.0, fat_per_100g: 15.0, sugar_per_100g: 0.7, fiber_per_100g: 6.7, is_healthy: 1, huskylens_label_id: 15 },
  { id: 16, name: 'Almonds', calories_per_100g: 579, protein_per_100g: 21.0, fat_per_100g: 50.0, sugar_per_100g: 4.4, fiber_per_100g: 12.5, is_healthy: 1, huskylens_label_id: 16 },
  { id: 17, name: 'Oats', calories_per_100g: 389, protein_per_100g: 17.0, fat_per_100g: 7.0, sugar_per_100g: 1.0, fiber_per_100g: 10.6, is_healthy: 1, huskylens_label_id: 17 },
  { id: 18, name: 'White Bread', calories_per_100g: 265, protein_per_100g: 9.0, fat_per_100g: 3.2, sugar_per_100g: 5.0, fiber_per_100g: 2.7, is_healthy: 0, huskylens_label_id: 18 },
  { id: 19, name: 'Coca-Cola', calories_per_100g: 37, protein_per_100g: 0.0, fat_per_100g: 0.0, sugar_per_100g: 10.6, fiber_per_100g: 0.0, is_healthy: 0, huskylens_label_id: 19 },
  { id: 20, name: 'Butter', calories_per_100g: 717, protein_per_100g: 0.9, fat_per_100g: 81.0, sugar_per_100g: 0.1, fiber_per_100g: 0.0, is_healthy: 0, huskylens_label_id: 20 },
  { id: 21, name: 'Spinach', calories_per_100g: 23, protein_per_100g: 2.9, fat_per_100g: 0.4, sugar_per_100g: 0.4, fiber_per_100g: 2.2, is_healthy: 1, huskylens_label_id: 21 },
  { id: 22, name: 'Lentils', calories_per_100g: 116, protein_per_100g: 9.0, fat_per_100g: 0.4, sugar_per_100g: 1.8, fiber_per_100g: 7.9, is_healthy: 1, huskylens_label_id: 22 },
  { id: 23, name: 'Pizza (plain)', calories_per_100g: 266, protein_per_100g: 11.0, fat_per_100g: 10.0, sugar_per_100g: 3.6, fiber_per_100g: 2.3, is_healthy: 0, huskylens_label_id: 23 },
  { id: 24, name: 'Milk (whole)', calories_per_100g: 61, protein_per_100g: 3.2, fat_per_100g: 3.3, sugar_per_100g: 4.8, fiber_per_100g: 0.0, is_healthy: 1, huskylens_label_id: 24 },
  { id: 25, name: 'Strawberry', calories_per_100g: 32, protein_per_100g: 0.7, fat_per_100g: 0.3, sugar_per_100g: 4.9, fiber_per_100g: 2.0, is_healthy: 1, huskylens_label_id: 25 }
];

function readFoods() {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(initialFoods));
    return initialFoods.slice();
  }
  try {
    return JSON.parse(raw);
  } catch (e) {
    console.error('Failed to parse stored foods, resetting', e);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(initialFoods));
    return initialFoods.slice();
  }
}

function writeFoods(arr) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(arr));
}

function nextId(foods) {
  let max = 0;
  for (const f of foods) if (f.id && f.id > max) max = f.id;
  return max + 1;
}

function el(tag, attrs = {}, ...children) {
  const e = document.createElement(tag);
  for (const k of Object.keys(attrs)) {
    if (k === 'class') e.className = attrs[k];
    else if (k === 'checked') e.checked = attrs[k];
    else e.setAttribute(k, attrs[k]);
  }
  for (const c of children) {
    if (typeof c === 'string') e.appendChild(document.createTextNode(c));
    else if (c) e.appendChild(c);
  }
  return e;
}

function renderTable() {
  const foods = readFoods();
  const tbody = document.getElementById('foods-body');
  tbody.innerHTML = '';
  for (const f of foods) {
    const tr = document.createElement('tr');
    const name = el('input', { value: f.name });
    const calories = el('input', { value: f.calories_per_100g, type: 'number', step: 'any' });
    const protein = el('input', { value: f.protein_per_100g, type: 'number', step: 'any' });
    const fat = el('input', { value: f.fat_per_100g, type: 'number', step: 'any' });
    const sugar = el('input', { value: f.sugar_per_100g, type: 'number', step: 'any' });
    const fiber = el('input', { value: f.fiber_per_100g, type: 'number', step: 'any' });
    const healthy = el('input', { type: 'checkbox' }); healthy.checked = !!f.is_healthy;
    const husky = el('input', { value: f.huskylens_label_id || '', type: 'number' });

    const saveBtn = el('button', {}, 'Save');
    saveBtn.addEventListener('click', () => {
      const foods = readFoods();
      const idx = foods.findIndex(x => x.id === f.id);
      if (idx === -1) return alert('Item not found');
      foods[idx] = {
        id: f.id,
        name: name.value,
        calories_per_100g: Number(calories.value) || 0,
        protein_per_100g: Number(protein.value) || 0,
        fat_per_100g: Number(fat.value) || 0,
        sugar_per_100g: Number(sugar.value) || 0,
        fiber_per_100g: Number(fiber.value) || 0,
        is_healthy: healthy.checked ? 1 : 0,
        huskylens_label_id: husky.value ? Number(husky.value) : null
      };
      writeFoods(foods);
      renderTable();
    });

    const delBtn = el('button', {}, 'Delete');
    delBtn.addEventListener('click', () => {
      if (!confirm('Delete ' + f.name + '?')) return;
      let foods = readFoods();
      foods = foods.filter(x => x.id !== f.id);
      writeFoods(foods);
      renderTable();
    });

    const actions = el('td');
    actions.appendChild(saveBtn);
    actions.appendChild(delBtn);

    tr.appendChild(el('td', {}, name));
    tr.appendChild(el('td', {}, calories));
    tr.appendChild(el('td', {}, protein));
    tr.appendChild(el('td', {}, fat));
    tr.appendChild(el('td', {}, sugar));
    tr.appendChild(el('td', {}, fiber));
    tr.appendChild(el('td', {}, healthy));
    tr.appendChild(el('td', {}, husky));
    tr.appendChild(actions);
    tbody.appendChild(tr);
  }
}

document.getElementById('refresh').addEventListener('click', renderTable);
document.getElementById('add-form').addEventListener('submit', (e) => {
  e.preventDefault();
  const form = e.target;
  const data = new FormData(form);
  const foods = readFoods();
  const newId = nextId(foods);
  const item = {
    id: newId,
    name: data.get('name'),
    calories_per_100g: Number(data.get('calories_per_100g') || 0),
    protein_per_100g: Number(data.get('protein_per_100g') || 0),
    fat_per_100g: Number(data.get('fat_per_100g') || 0),
    sugar_per_100g: Number(data.get('sugar_per_100g') || 0),
    fiber_per_100g: Number(data.get('fiber_per_100g') || 0),
    is_healthy: form.is_healthy.checked ? 1 : 0,
    huskylens_label_id: data.get('huskylens_label_id') ? Number(data.get('huskylens_label_id')) : null
  };
  foods.push(item);
  writeFoods(foods);
  form.reset();
  renderTable();
});

document.getElementById('export').addEventListener('click', () => {
  const foods = readFoods();
  // build python FOODS = [ ... ]
  let out = 'FOODS = [\n';
  for (const f of foods) {
    const name = String(f.name).replace(/"/g, '\\"');
    const husky = (f.huskylens_label_id === null || f.huskylens_label_id === undefined) ? 'None' : f.huskylens_label_id;
    out += `    ("${name}", ${f.calories_per_100g}, ${f.protein_per_100g}, ${f.fat_per_100g}, ${f.sugar_per_100g}, ${f.fiber_per_100g}, ${f.is_healthy}, ${husky}),\n`;
  }
  out += ']\n';
  const blob = new Blob([out], { type: 'text/x-python' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'foods.py';
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
});

// initial render
renderTable();
