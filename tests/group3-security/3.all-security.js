/**
 * =====================================================================
 *  TEST 3.1+3.2+3.3 — SECURITY: JWT, RBAC, SQL INJECTION
 * =====================================================================
 *
 * MỤC ĐÍCH:
 *   Kiểm tra toàn bộ lớp bảo mật của hệ thống:
 *   3.1 - JWT Token hết hạn/sai → 401
 *   3.2 - User thường gọi API Admin → 403
 *   3.3 - Input độc hại (SQL Injection) → không có lỗ hổng
 *
 * CHẠY: node tests/group3-security/3.all-security.js
 * =====================================================================
 */

const cfg = require('../config');

async function call(label, url, options, expectedStatus) {
  try {
    const res = await fetch(url, options);
    const body = await res.json().catch(() => ({}));
    const ok = res.status === expectedStatus;
    return { label, status: res.status, expected: expectedStatus, ok, body };
  } catch (err) {
    return { label, status: 'ERR', expected: expectedStatus, ok: false, body: { error: err.message } };
  }
}

async function runSection(title, tests) {
  console.log(`\n  ═══ ${title} ═══`);
  let pass = 0; let fail = 0;
  for (const t of tests) {
    const r = await call(t.label, t.url, t.options, t.expectedStatus);
    const icon = r.ok ? '✅' : '❌';
    console.log(`  ${icon} ${r.label}`);
    console.log(`     HTTP ${r.status} (mong đợi: ${r.expected}) ${r.ok ? '→ PASS' : '→ FAIL'}`);
    if (!r.ok) console.log(`     Body: ${JSON.stringify(r.body).substring(0, 80)}`);
    if (r.ok) pass++; else fail++;
    await new Promise(res => setTimeout(res, 100));
  }
  return { pass, fail };
}

async function run() {
  console.log('='.repeat(65));
  console.log('  TEST NHÓM 3 — SECURITY: JWT + RBAC + SQL INJECTION');
  console.log('='.repeat(65));

  let totalPass = 0; let totalFail = 0;

  // ─── 3.1: JWT Token Validation ─────────────────────────────────
  const jwtTests = [
    {
      label: 'Không có Authorization header',
      url: `${cfg.BASE_URL}/cart`,
      options: { method: 'GET' },
      expectedStatus: 401,
    },
    {
      label: 'Token format sai (không có Bearer)',
      url: `${cfg.BASE_URL}/orders/my-orders`,
      options: { method: 'GET', headers: { Authorization: 'eyJhbGciOiJIUzUxMiJ9.invalid' } },
      expectedStatus: 401,
    },
    {
      label: 'Token hết hạn (giả mạo expired)',
      url: `${cfg.BASE_URL}/cart`,
      options: {
        method: 'GET',
        headers: {
          Authorization: 'Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0QHRlc3QuY29tIiwiaWF0IjoxNjAwMDAwMDAwLCJleHAiOjE2MDAwMDAwMDF9.INVALID',
        },
      },
      expectedStatus: 401,
    },
    {
      label: 'Token hợp lệ → được phép',
      url: `${cfg.BASE_URL}/cart`,
      options: { method: 'GET', headers: { Authorization: cfg.TOKEN_USER1 } },
      expectedStatus: 200,
    },
  ];

  const jwt = await runSection('3.1 — JWT TOKEN VALIDATION', jwtTests);
  totalPass += jwt.pass; totalFail += jwt.fail;

  // ─── 3.2: Role-Based Access Control ──────────────────────────────
  const rbacTests = [
    {
      label: 'User gọi GET /admin/orders → 403',
      url: `${cfg.BASE_URL}/admin/orders`,
      options: { method: 'GET', headers: { Authorization: cfg.TOKEN_USER1 } },
      expectedStatus: 403,
    },
    {
      label: 'User gọi GET /admin/dashboard → 403',
      url: `${cfg.BASE_URL}/admin/dashboard`,
      options: { method: 'GET', headers: { Authorization: cfg.TOKEN_USER1 } },
      expectedStatus: 403,
    },
    {
      label: 'User gọi DELETE /admin/products/1 → 403',
      url: `${cfg.BASE_URL}/admin/products/1`,
      options: { method: 'DELETE', headers: { Authorization: cfg.TOKEN_USER1 } },
      expectedStatus: 403,
    },
    {
      label: 'Admin gọi GET /admin/orders → 200 (nếu có TOKEN_ADMIN)',
      url: `${cfg.BASE_URL}/admin/orders`,
      options: {
        method: 'GET',
        headers: { Authorization: cfg.TOKEN_ADMIN !== 'Bearer YOUR_JWT_TOKEN_ADMIN_HERE' ? cfg.TOKEN_ADMIN : cfg.TOKEN_USER1 },
      },
      expectedStatus: cfg.TOKEN_ADMIN !== 'Bearer YOUR_JWT_TOKEN_ADMIN_HERE' ? 200 : 403,
    },
  ];

  const rbac = await runSection('3.2 — ROLE-BASED ACCESS CONTROL (RBAC)', rbacTests);
  totalPass += rbac.pass; totalFail += rbac.fail;

  // ─── 3.3: SQL Injection Prevention ──────────────────────────────
  const sqlTests = [
    {
      label: "Search với ' OR '1'='1",
      url: `${cfg.BASE_URL}/products?search=' OR '1'='1`,
      options: { method: 'GET' },
      expectedStatus: 200, // Phải trả về bình thường (không crash, không leak dữ liệu)
    },
    {
      label: "Search với '; DROP TABLE products;--",
      url: `${cfg.BASE_URL}/products?search='; DROP TABLE products;--`,
      options: { method: 'GET' },
      expectedStatus: 200,
    },
    {
      label: 'Search với <script>alert(1)</script>',
      url: `${cfg.BASE_URL}/products?search=${encodeURIComponent('<script>alert(1)</script>')}`,
      options: { method: 'GET' },
      expectedStatus: 200,
    },
    {
      label: 'Login với username: admin@test.com\'--',
      url: `${cfg.BASE_URL}/auth/login`,
      options: {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: "admin@test.com'--", password: 'anything' }),
      },
      expectedStatus: 401, // Phải từ chối, không bypass được auth
    },
  ];

  const sql = await runSection('3.3 — SQL INJECTION PREVENTION', sqlTests);

  // Kiểm tra thêm: response không chứa stack trace SQL
  console.log('\n  ─ Kiểm tra response không leak stack trace... ─');
  const testRes = await fetch(`${cfg.BASE_URL}/products?search=' OR 1=1--`);
  const testBody = await testRes.text();
  const hasStackTrace = testBody.includes('at com.example') || testBody.includes('HibernateException');
  console.log(`  ${hasStackTrace ? '❌' : '✅'} Response ${hasStackTrace ? 'CÓ' : 'KHÔNG'} chứa stack trace/exception detail`);

  totalPass += sql.pass + (hasStackTrace ? 0 : 1);
  totalFail += sql.fail + (hasStackTrace ? 1 : 0);

  // ─── Tổng kết ────────────────────────────────────────────────────
  console.log('\n' + '═'.repeat(65));
  console.log(`  TỔNG KẾT SECURITY: ${totalPass} PASS / ${totalFail} FAIL`);
  if (totalFail === 0) {
    console.log('\n  ✅ PASS — Tất cả kiểm tra bảo mật đều PASS!');
    console.log('  → JWT Filter chặn đúng request không hợp lệ.');
    console.log('  → RBAC phân quyền đúng ADMIN vs CUSTOMER.');
    console.log('  → JPA Parameterized Query ngăn SQL Injection hoàn toàn.');
  } else {
    console.log(`\n  ❌ ${totalFail} test FAIL — Kiểm tra lại cấu hình security.`);
  }
  console.log('='.repeat(65));
}

run().catch(console.error);
