document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const transactionId = urlParams.get('transactionId');

    if (!transactionId) {
        document.getElementById('payment-status').innerText = 'Error: No transaction ID provided.';
        return;
    }

    // Fetch transaction details
    fetch(`/api/payment/transaction/${transactionId}`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            // Include authorization header if needed (e.g., JWT token)
        },
    })
    .then(response => response.json())
    .then(data => {
        document.getElementById('transaction-id').innerText = data.transactionId;
        document.getElementById('mobile-number').innerText = data.mobileNumber;
        document.getElementById('plan-name').innerText = data.rechargePlan.planName;
        document.getElementById('amount').innerText = data.amount;
    })
    .catch(error => {
        console.error('Error fetching transaction:', error);
        document.getElementById('payment-status').innerText = 'Error loading transaction details.';
    });

    // Handle Razorpay payment
    document.getElementById('pay-with-razorpay').addEventListener('click', () => {
        fetch(`/api/payment/create-order?transactionId=${transactionId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
        })
        .then(response => response.json())
        .then(order => {
            const options = {
                key: 'rzp_test_4IYAWUWS39VDbM', // Replace with your Razorpay Key ID
                amount: order.amount,
                currency: order.currency,
                name: 'MobiComm',
                description: `Recharge for Transaction ID: ${order.transactionId}`,
                order_id: order.orderId,
                handler: function (response) {
                    // Verify payment on success
                    verifyPayment(response, order.transactionId);
                },
                prefill: {
                    name: 'Customer Name', // You can fetch this dynamically
                    email: 'customer@example.com',
                    contact: document.getElementById('mobile-number').innerText,
                },
                theme: {
                    color: '#007bff',
                },
            };

            const rzp = new Razorpay(options);
            rzp.open();
        })
        .catch(error => {
            console.error('Error creating Razorpay order:', error);
            document.getElementById('payment-status').innerText = 'Error initiating payment.';
        });
    });
});

function verifyPayment(response, transactionId) {
    const verificationData = {
        razorpayPaymentId: response.razorpay_payment_id,
        razorpayOrderId: response.razorpay_order_id,
        razorpaySignature: response.razorpay_signature,
        transactionId: transactionId,
    };

    fetch('/api/payment/verify', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams(verificationData).toString(),
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'Payment successful') {
            document.getElementById('payment-status').innerText = 'Payment successful! Redirecting...';
            setTimeout(() => {
                window.location.href = '/success?transactionId=' + transactionId;
            }, 2000);
        } else {
            document.getElementById('payment-status').innerText = 'Payment verification failed.';
        }
    })
    .catch(error => {
        console.error('Error verifying payment:', error);
        document.getElementById('payment-status').innerText = 'Error verifying payment.';
    });
}