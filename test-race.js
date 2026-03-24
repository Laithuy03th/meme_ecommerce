const URL = 'http://localhost:8080/api/v1/orders/checkout';
// TODO: Dán Bearer token thật của bạn vào đây
const TOKEN = 'Bearer eyJhbGciOi...';

async function testRaceCondition() {
    console.log("Bắt đầu bắn 10 luồng cùng lúc...");

    // Tạo 10 request với 10 mã idempotencyKey khác nhau
    const requests = Array.from({ length: 10 }).map((_, idx) => {
        return fetch(URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': TOKEN
            },
            body: JSON.stringify({
                addressId: 1, // Đổi addressId mồi (nếu cần)
                shippingMethodId: 1, // Đổi shippingMethodId mồi (nếu cần)
                paymentMethod: "COD",
                idempotencyKey: "test-race-key-" + Math.random() // Lách tầng Idempotency
            })
        });
    });

    // Promise.all ép 10 request khởi hành cùng đúng 1 mili-giây
    const responses = await Promise.all(requests);

    // In kết quả
    for (let i = 0; i < responses.length; i++) {
        const text = await responses[i].text();
        console.log(`Luồng ${i + 1} - Status: ${responses[i].status} - Lỗi/Thành công: ${text.substring(0, 150)}`);
    }
}

testRaceCondition();
