let catalogRequest = 0;
const money = value => Number(value).toLocaleString('vi-VN') + '₫';
const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[character]));

function toast(message) {
  const element = document.getElementById('toast');
  if (!element) return;
  element.textContent = message;
  element.classList.add('show');
  clearTimeout(window._toastTimer);
  window._toastTimer = setTimeout(() => element.classList.remove('show'), 2600);
}

async function updateCartCount() {
  try {
    const response = await fetch('/cart/count');
    if (response.ok && !response.redirected) {
      const count = await response.json();
      document.querySelectorAll('.cart-count').forEach(element => element.textContent = count);
    }
  } catch (error) { /* Keep the existing count visible while offline. */ }
}

async function addCart(id, quantity = 1) {
  const token = document.getElementById('csrfToken')?.dataset.token;
  if (!token) { location.href = '/login'; return; }
  try {
    const response = await fetch('/cart/add', {
      method: 'POST', credentials: 'same-origin',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-CSRF-TOKEN': token },
      body: new URLSearchParams({ productId: id, quantity: String(quantity) })
    });
    if (response.redirected) { location.href = new URL(response.url).pathname; return; }
    if (!response.ok) throw new Error('Không thêm được vào giỏ hàng');
    await updateCartCount();
    toast('Đã thêm vào giỏ hàng!');
  } catch (error) { toast('Không thêm được vào giỏ hàng. Kiểm tra số lượng và tồn kho.'); }
}

function productImage(product) {
  return product.imageUrl
    ? `<img src="${escapeHtml(product.imageUrl)}" alt="${escapeHtml(product.name)}" loading="lazy" style="max-width:95%;max-height:140px;object-fit:contain">`
    : '<span aria-label="Chưa có ảnh sản phẩm">▱</span>';
}

function productCard(product) {
  const url = `/product-detail?id=${encodeURIComponent(product.id)}`;
  return `<article class="product"><div class="product-art"><a class="product-art-link" href="${url}">${productImage(product)}</a></div><h3>${escapeHtml(product.name)}</h3><div><span class="price">${money(product.price)}</span></div><div class="rating">${escapeHtml(product.brand || 'TechStore')} · ${escapeHtml(product.category)}</div><button class="add-btn" data-add="${escapeHtml(product.id)}">＋ Thêm vào giỏ</button><a class="detail-link" href="${url}">Xem chi tiết ↗</a></article>`;
}

function flashCard(product) {
  const url = `/product-detail?id=${encodeURIComponent(product.id)}`;
  return `<article class="flash-card"><a class="flash-art" href="${url}">${productImage(product)}</a><div class="flash-name">${escapeHtml(product.name)}</div><div><span class="flash-price">${money(product.price)}</span></div><div class="rating">${escapeHtml(product.brand || 'TechStore')} · ${escapeHtml(product.category)}</div><a href="${url}">Xem chi tiết ↗</a></article>`;
}

async function loadProducts(targetId, sort = 'newest', renderCard = productCard) {
  const target = document.getElementById(targetId);
  if (!target) return;
  const requestId = ++catalogRequest;
  target.dataset.requestId = String(requestId);
  target.setAttribute('aria-busy', 'true');
  target.innerHTML = '<p class="catalog-load-message" role="status">Đang tải sản phẩm…</p>';
  try {
    const response = await fetch(`/api/products?size=5&sort=${encodeURIComponent(sort)}`, { headers: { Accept: 'application/json' } });
    if (!response.ok) throw new Error('Danh sách sản phẩm không khả dụng');
    const payload = await response.json();
    if (!Array.isArray(payload.content)) throw new Error('Dữ liệu sản phẩm không hợp lệ');
    if (target.dataset.requestId !== String(requestId)) return;
    target.innerHTML = payload.content.length
      ? payload.content.map(renderCard).join('')
      : '<p class="catalog-load-message" role="status">Chưa có sản phẩm đang kinh doanh.</p>';
  } catch (error) {
    if (target.dataset.requestId !== String(requestId)) return;
    target.innerHTML = '<div class="catalog-load-message" role="alert"><p>Chưa tải được sản phẩm. Vui lòng thử lại.</p><button type="button" class="outline-btn" data-retry-products>Tải lại</button></div>';
    target.querySelector('[data-retry-products]')?.addEventListener('click', () => loadProducts(targetId, sort, renderCard));
  } finally {
    if (target.dataset.requestId === String(requestId)) target.removeAttribute('aria-busy');
  }
}

function initShared() {
  updateCartCount();
  const categoryButton = document.getElementById('categoryBtn');
  const menu = document.getElementById('megaMenu');
  categoryButton?.addEventListener('click', () => menu?.classList.toggle('open'));
  document.addEventListener('click', event => {
    if (menu && !event.target.closest('.mega-wrap') && !event.target.closest('#categoryBtn')) menu.classList.remove('open');
  });
  const navigation = document.getElementById('mainNav');
  document.getElementById('mobileMenu')?.addEventListener('click', () => navigation?.classList.toggle('open'));
  document.getElementById('searchForm')?.addEventListener('submit', event => {
    event.preventDefault();
    const query = document.getElementById('searchInput')?.value.trim() || '';
    location.href = '/products?q=' + encodeURIComponent(query);
  });
  document.getElementById('subscribeForm')?.addEventListener('submit', event => {
    event.preventDefault();
    toast('Đăng ký nhận tin sẽ sớm ra mắt.');
  });
  document.body.addEventListener('click', async event => {
    const button = event.target.closest('[data-add]');
    if (!button || button.disabled) return;
    button.disabled = true;
    try { await addCart(button.dataset.add); }
    finally { button.disabled = false; }
  });
}

function initHome() {
  loadProducts('flashProducts', 'newest', flashCard);
  loadProducts('homeProducts', 'newest');
  document.querySelectorAll('[data-collection]').forEach(button => button.addEventListener('click', () => {
    document.querySelectorAll('[data-collection]').forEach(item => item.classList.toggle('active', item === button));
    loadProducts('homeProducts', button.dataset.collection);
  }));
}

function initMisc() {
  document.getElementById('contactForm')?.addEventListener('submit', event => {
    event.preventDefault();
    toast('Gửi lời nhắn trực tuyến sẽ sớm ra mắt. Vui lòng liên hệ cửa hàng để được hỗ trợ.');
  });
  document.querySelectorAll('[data-demo-action]').forEach(button => button.addEventListener('click', () => toast('Tính năng này sẽ sớm ra mắt.')));
}

document.addEventListener('DOMContentLoaded', () => {
  initShared();
  if (document.getElementById('homeProducts')) initHome();
  if (document.getElementById('offerProducts')) loadProducts('offerProducts', 'newest');
  initMisc();
});
