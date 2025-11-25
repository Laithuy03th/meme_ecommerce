# Project Summary for Swagger Update

## Required Swagger Updates

### Add Tags/Groups to organize endpoints:

```yaml
tags:
  - name: "CLIENT - Authentication"
    description: "User authentication endpoints (Register, Login, Logout)"
  - name: "CLIENT - Profile & Addresses"
    description: "User profile and address management"
  - name: "CLIENT - Products & Catalog"
    description: "Browse products, categories, search"
  - name: "CLIENT - Cart"
    description: "Shopping cart management"
  - name: "CLIENT - Wishlist"
    description: "Favorite products management"
  - name: "CLIENT - Orders"
    description: "Order placement and tracking"
  - name: "CLIENT - Reviews"
    description: "Product reviews and ratings"
  - name: "CLIENT - Vouchers"
    description: "Discount voucher validation"
  - name: "ADMIN - Dashboard"
    description: "Admin statistics and analytics"
  - name: "ADMIN - User Management"
    description: "Manage users, roles, permissions"
  - name: "ADMIN - Product Management"
    description: "CRUD operations for products"
  - name: "ADMIN - Order Management"
    description: "View and update order status"
  - name: "ADMIN - Voucher Management"
    description: "Create and manage discount vouchers"
  - name: "ADMIN - Review Moderation"
    description: "Moderate user reviews"
```

### Add Dashboard Stats endpoint:

```yaml
/api/v1/admin/dashboard/stats:
  get:
    tags:
      - "ADMIN - Dashboard"
    summary: Get dashboard statistics
    responses:
      '200':
        description: Success
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DashboardStatsResponse'
```

### Add DashboardStatsResponse schema:

```yaml
DashboardStatsResponse:
  type: object
  properties:
    totalRevenue:
      type: number
      format: double
    todayRevenue:
      type: number
      format: double
    monthRevenue:
      type: number
      format: double
    totalOrders:
      type: integer
      format: int64
    pendingOrders:
      type: integer
      format: int64
    shippingOrders:
      type: integer
      format: int64
    completedOrders:
      type: integer
      format: int64
    canceledOrders:
      type: integer
      format: int64
    totalCustomers:
      type: integer
      format: int64
    newCustomersThisMonth:
      type: integer
      format: int64
    totalProducts:
      type: integer
      format: int64
    lowStockProducts:
      type: integer
      format: int64
    topSellingProducts:
      type: array
      items:
        $ref: '#/components/schemas/TopProductDto'

TopProductDto:
  type: object
  properties:
    productId:
      type: integer
      format: int64
    productName:
      type: string
    totalSold:
      type: integer
      format: int64
    revenue:
      type: number
      format: double
```

### Add Cancel Order endpoint:

```yaml
/api/v1/users/me/orders/{orderId}/cancel:
  put:
    tags:
      - "CLIENT - Orders"
    summary: Cancel order (only PENDING status)
    parameters:
      - in: path
        name: orderId
        required: true
        schema:
          type: integer
          format: int64
    responses:
      '200':
        description: Order canceled successfully
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/OrderResponse'
      '400':
        description: Cannot cancel order (not PENDING status)
      '401':
        $ref: '#/components/responses/Unauthorized'
      '404':
        $ref: '#/components/responses/NotFound'
```
