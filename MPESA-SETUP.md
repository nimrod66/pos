# M-Pesa Setup (Daraja STK Push)

The POS supports two M-Pesa modes. It picks one automatically based on configuration:

| Mode | When | Flow |
|------|------|------|
| **STK Push** | `MPESA_CONSUMER_KEY`, `MPESA_CONSUMER_SECRET`, `MPESA_PASSKEY`, `MPESA_CALLBACK_URL` are all set | A payment prompt pops on the customer's phone; the POS polls until it completes |
| **MANUAL** (fallback) | Any of the above missing | Customer pays via their own phone; cashier types the M-Pesa confirmation code |

## Enabling STK Push (Sandbox)

1. Sign up at [developer.safaricom.co.ke](https://developer.safaricom.co.ke) (Daraja).
2. Create a new app and copy the **Consumer Key** and **Consumer Secret**.
3. On the app page, open *Lipa na M-Pesa Online* test credentials and copy the **Passkey**.
4. Fill in `MPESA_*` values in `.env.pilot` (leave `MPESA_SHORTCODE=174379` for sandbox).
5. Restart the API:
   ```
   docker compose -f docker-compose.pilot.yml --env-file .env.pilot up -d api
   ```

Verify: log in to the POS, start a checkout, choose M-Pesa - the UI will
show the STK phone-number prompt instead of the manual reference box.

> Note: gateway callbacks are disabled in this build (signature verification
> pending). STK payments complete by status polling, which the POS handles
> automatically. Refunds are always processed manually outside the gateway.

## Going live (production)

Set `MPESA_ENVIRONMENT=production`, swap in your production shortcode and
credentials from the Daraja production app approval, and point
`MPESA_CALLBACK_URL` at a publicly reachable HTTPS endpoint.
