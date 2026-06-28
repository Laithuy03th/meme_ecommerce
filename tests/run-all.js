/**
 * =====================================================================
 *  RUN ALL TESTS — CHẠY TOÀN BỘ TEST SUITE
 * =====================================================================
 *  CHẠY: node tests/run-all.js
 * =====================================================================
 */

const { execSync } = require('child_process');
const path = require('path');

const allTests = [
  // Nhóm 1: E-Commerce
  { file: 'group1-ecommerce/1.1-race-stock.js', name: 'Race Condition Stock' },
  { file: 'group1-ecommerce/1.2-race-voucher.js', name: 'Race Condition Voucher' },
  { file: 'group1-ecommerce/1.3-idempotency.js', name: 'Idempotency Key' },
  { file: 'group1-ecommerce/1.4-deadlock-prevention.js', name: 'Deadlock Prevention' },
  { file: 'group1-ecommerce/1.5-vnpay-flow.js', name: 'VNPAY Flow' },
  { file: 'group1-ecommerce/1.7-cancel-stock-rollback.js', name: 'Cancel Stock Rollback' },
  { file: 'group1-ecommerce/1.8-auth-cart.js', name: 'Auth & Cart Security' },
  { file: 'group1-ecommerce/1.9-review-guard.js', name: 'Review Guard' },
  { file: 'group1-ecommerce/1.10-voucher-test.js', name: 'Voucher Test' },
  // Nhóm 2: Chatbot
  { file: 'group2-chatbot/2.1-unknown-product.js', name: 'Chatbot Unknown Product' },
  { file: 'group2-chatbot/2.2-product-image-sync.js', name: 'Chatbot Image Sync' },
  { file: 'group2-chatbot/2.3-conversation-context.js', name: 'Chatbot Context' },
  { file: 'group2-chatbot/2.4-auth-order-tracking.js', name: 'Chatbot Auth Order' },
  // Nhóm 3: Security
  { file: 'group3-security/3.all-security.js', name: 'All Security Tests' },
];

async function runTest(testFile, testName) {
  console.log(`\n${'─'.repeat(65)}`);
  console.log(`▶  CHẠY: ${testName} (${testFile})`);
  console.log('─'.repeat(65));
  try {
    const output = execSync(`node ${path.join(__dirname, testFile)}`, {
      encoding: 'utf8',
      timeout: 120000, // 2 phút timeout mỗi test
    });
    console.log(output);
    return true;
  } catch (err) {
    console.error(`❌ Lỗi khi chạy ${testFile}:`);
    console.error(err.stdout || err.message);
    return false;
  }
}

async function main() {
  console.log('╔' + '═'.repeat(63) + '╗');
  console.log('║   MEME SHOP — FULL TEST SUITE                               ║');
  console.log('╚' + '═'.repeat(63) + '╝');
  console.log(`\n  Tổng số test: ${allTests.length}`);
  console.log('  Bắt đầu lúc: ' + new Date().toLocaleString('vi-VN'));
  console.log('\n  ⚠️  LƯU Ý TRƯỚC KHI CHẠY:');
  console.log('  1. Backend phải đang chạy tại port 8080');
  console.log('  2. Điền đầy đủ TOKEN_USER1, TOKEN_ADMIN trong tests/config.js');
  console.log('  3. Set PRODUCT_ID, VARIANT_ID, ADDRESS_ID cho đúng DB');
  console.log('  4. Test 1.1 cần set stock = 1, Test 1.2 cần set usageLimit = 1');

  const skip = process.argv.includes('--skip-concurrent');
  const onlyGroup = process.argv.find(a => a.startsWith('--group='))?.split('=')[1];

  let pass = 0; let fail = 0;

  const testsToRun = allTests.filter(t => {
    if (onlyGroup) return t.file.startsWith(`group${onlyGroup}`);
    if (skip && t.file.includes('concurrent')) return false;
    return true;
  });

  for (const t of testsToRun) {
    const ok = await runTest(t.file, t.name);
    if (ok) pass++; else fail++;
  }

  console.log('\n' + '╔' + '═'.repeat(63) + '╗');
  console.log(`║   KẾT QUẢ: ${pass} PASS / ${fail} FAIL / ${testsToRun.length} TỔNG        `);
  console.log('╚' + '═'.repeat(63) + '╝');
}

main().catch(console.error);
