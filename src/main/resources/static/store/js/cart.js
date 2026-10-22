(() => {
  'use strict';
  const page = document.getElementById('cartPage');
  if (!page || !window.fetch) return;
  const checkout = document.getElementById('cartCheckout');
  const hint = document.getElementById('cartCheckoutHint');
  const message = document.getElementById('cartLiveMessage');
  const rows = new Map();
  const pending = new Map();
  let timer;
  let processing = false;
  let uncertain = false;
  let checkoutAllowed = page.dataset.checkoutAllowed === 'true';
  const formatMoney = value => Number(value).toLocaleString('vi-VN') + '₫';

  const showMessage = (text, error = false) => {
    message.textContent = text;
    message.hidden = false;
    message.classList.toggle('cart-error', error);
  };
  const isPending = () => processing || pending.size > 0 || [...rows.values()].some(state => state.dirty);
  const updateCheckout = () => {
    if (!checkout) return;
    const allowed = checkoutAllowed && !isPending() && !uncertain;
    checkout.classList.toggle('is-disabled', !allowed);
    checkout.setAttribute('aria-disabled', String(!allowed));
    if (allowed) checkout.setAttribute('href', '/checkout');
    else checkout.removeAttribute('href');
    if (hint && uncertain) hint.textContent = 'Cần tải lại giỏ hàng để xác nhận số lượng đã lưu.';
    else if (hint && isPending()) hint.textContent = 'Đang lưu số lượng. Vui lòng chờ trước khi đặt hàng.';
  };

  function applyCart(cart) {
    checkoutAllowed = cart.checkoutAllowed === true;
    const existing = new Set(cart.items.map(item => String(item.productId)));
    for (const [id, state] of rows) {
      if (!existing.has(id)) {
        state.row.remove();
        pending.delete(id);
        rows.delete(id);
      }
    }
    for (const item of cart.items) {
      const state = rows.get(String(item.productId));
      if (!state) continue;
      if (!state.dirty) state.input.value = item.quantity;
      state.row.querySelector('.cart-line-total').textContent = formatMoney(item.subtotal);
      const stockNote = state.row.querySelector('.cart-stock-note');
      stockNote.textContent = item.availableStock == null
        ? 'Chọn chi nhánh để kiểm tra tồn kho'
        : `Còn ${item.availableStock} sản phẩm tại chi nhánh`;
      stockNote.classList.toggle('stock-shortage', item.availableStock != null && item.availableStock < item.quantity);
    }
    page.querySelectorAll('[data-cart-subtotal]').forEach(element => element.textContent = formatMoney(cart.subtotal));
    document.querySelectorAll('.cart-count').forEach(element => element.textContent = cart.items.reduce((total, item) => total + item.quantity, 0));
    if (hint) hint.textContent = !cart.branchId
      ? 'Hãy chọn chi nhánh để kiểm tra tồn kho trước khi đặt hàng.'
      : (checkoutAllowed ? 'Tồn kho sẽ được kiểm tra lại khi bạn xác nhận đặt hàng.' : 'Vui lòng giảm số lượng sản phẩm thiếu hàng trước khi đặt hàng.');
    if (!cart.items.length) location.reload();
  }

  async function drain() {
    if (processing || uncertain) return;
    processing = true;
    updateCheckout();
    try {
      while (pending.size && !uncertain) {
        const [id, request] = pending.entries().next().value;
        pending.delete(id);
        const state = rows.get(id);
        if (!state) continue;
        try {
          const response = await fetch('/cart/quantity', {
            method: 'POST', credentials: 'same-origin',
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded', Accept: 'application/json',
              [page.dataset.csrfHeader]: page.dataset.csrfToken
            },
            body: new URLSearchParams({ productId: id, quantity: String(request.quantity) })
          });
          if (response.redirected) { location.href = response.url; return; }
          if (!(response.headers.get('Content-Type') || '').includes('application/json')) throw new Error('Chưa xác nhận được cập nhật');
          const payload = await response.json();
          if (!response.ok && !payload.cart) throw new Error('Chưa xác nhận được cập nhật');
          if (state.version === request.version) {
            state.dirty = false;
            state.row.classList.remove('is-saving');
            state.input.disabled = false;
          }
          applyCart(response.ok ? payload : payload.cart);
          showMessage(response.ok ? (request.quantity === 0 ? 'Đã xóa sản phẩm khỏi giỏ.' : 'Đã cập nhật giỏ hàng.') : payload.error, !response.ok);
        } catch (error) {
          uncertain = true;
          pending.clear();
          showMessage('Chưa xác nhận được số lượng đã lưu. Vui lòng tải lại giỏ hàng hoặc bấm Cập nhật để gửi lại.', true);
          const reload = document.createElement('a');
          reload.href = '/cart';
          reload.textContent = ' Tải lại giỏ hàng';
          message.appendChild(reload);
          for (const entry of rows.values()) entry.input.disabled = false;
        }
      }
    } finally {
      processing = false;
      updateCheckout();
    }
  }

  function schedule(state, quantity, immediate = false) {
    if (!Number.isInteger(quantity) || quantity < 0 || quantity > 99) return;
    state.version++;
    state.dirty = true;
    state.row.classList.add('is-saving');
    pending.set(state.row.dataset.productId, { quantity, version: state.version });
    clearTimeout(timer);
    updateCheckout();
    if (immediate) drain();
    else timer = setTimeout(drain, 400);
  }

  page.querySelectorAll('.cart-row[data-product-id]').forEach(row => {
    const input = row.querySelector('.cart-quantity');
    const state = { row, input, dirty: false, version: 0 };
    rows.set(row.dataset.productId, state);
    input.addEventListener('input', () => {
      if (uncertain) return;
      if (input.validity.valid) schedule(state, Number(input.value));
      else {
        pending.delete(row.dataset.productId);
        state.version++;
        state.dirty = true;
        showMessage('Số lượng phải là số nguyên từ 1 đến 99. Bạn có thể bấm Xóa để bỏ sản phẩm.', true);
        updateCheckout();
      }
    });
    row.querySelector('.cart-update-form').addEventListener('submit', event => {
      if (uncertain) return;
      event.preventDefault();
      if (input.reportValidity()) schedule(state, Number(input.value), true);
    });
    row.querySelector('.cart-remove-form').addEventListener('submit', event => {
      if (uncertain) return;
      event.preventDefault();
      input.disabled = true;
      schedule(state, 0, true);
    });
  });

  checkout?.addEventListener('click', event => {
    if (checkout.getAttribute('aria-disabled') === 'true') {
      event.preventDefault();
      showMessage(uncertain ? 'Hãy tải lại giỏ hàng để kiểm tra dữ liệu đã lưu.' : 'Vui lòng chọn chi nhánh và chờ lưu đủ số lượng hợp lệ trước khi đặt hàng.', true);
    }
  });
  page.querySelector('.cart-branch-form')?.addEventListener('submit', event => {
    if (!uncertain && isPending()) {
      event.preventDefault();
      showMessage('Vui lòng chờ lưu số lượng sản phẩm trước khi đổi chi nhánh.', true);
      clearTimeout(timer);
      drain();
    }
  });
})();
