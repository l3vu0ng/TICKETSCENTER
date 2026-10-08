# Identity schema and handoff

| Mapping | SQL | Notes |
|---|---|---|
| User.id | uniqueidentifier PK | UUID, immutable |
| email / normalizedEmail | nvarchar(254), NOT NULL | normalized unique index |
| userName / normalizedUserName | nvarchar(32), NOT NULL | normalized unique index |
| fullName / phone | nvarchar(120) / nvarchar(20) nullable | NFC name, validated phone |
| status / platformRole | nvarchar(20) | ACTIVE/DISABLED; CUSTOMER/ADMIN |
| emailVerified | bit NOT NULL DEFAULT 0 | Single verification state |
| passwordHash / authVersion | nvarchar(255) / int | Hidden from UserDto |
| createdAt / version | datetime2(7) / int | UTC / optimistic version |
| OtpRecord | technical entity | keyed digest, purpose, 0–5 attempts, lifetime, consumption/invalidation |
| ResetGrant | technical entity | hashed session binding, lifetime, one-use consumedAt |

UserDto exposes only id, userName, fullName, email, nullable phone, status,
emailVerified and platformRole. User creation is CUSTOMER/ACTIVE/unverified.
Profile validation is atomic; an invalid phone cannot partially change the name.
A verified DISABLED account is not eligible to buy.

Preserve 0010 and apply 0011 through the reviewed manifest. New checks enforce
normalization/length/authVersion and OTP/reset bounds. Normalized columns use explicit
Latin1_General_100_BIN2 collation. No OTP/grant stores a raw code or reset token.

Cross-domain reference exports: dbo.[User](id) for Organization requester/membership,
Order buyer and TicketHold buyer. Đông owns UserOrganizationRole and its active/history
mapping. Current temporary `Map<Object,OrganizationRole>` remains a known contract
gap until Organization exists; it is transient and is not accepted as the diagram's
persisted `Map<Organization,OrganizationRole>`.

ProfileDto's `Page<?>` similarly requires replacement with Đông's exact MembershipDto.
These two type/mapping integrations are explicitly deferred by the user.
