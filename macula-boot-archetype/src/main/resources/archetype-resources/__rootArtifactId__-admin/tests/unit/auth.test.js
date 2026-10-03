import {beforeEach, describe, expect, it, vi} from "vitest"

vi.mock("@/utils/request", () => ({
    default: {
        post: vi.fn(),
        get: vi.fn()
    }
}))

import http from "@/utils/request"
import auth from "@/api/model/common/auth"
import config from "@/config"

describe("systemToken", () => {
    beforeEach(() => {
        http.post.mockReset()
        http.post.mockResolvedValue({access_token: "token"})
    })

    it("submits password grant as form data without duplicate notifications", async () => {
        await auth.systemToken.post({
            grant_type: "password",
            username: "rain",
            password: "secret"
        }, {
            timeout: 3000,
            headers: {
                "X-Request-Source": "login"
            }
        })

        expect(http.post).toHaveBeenCalledOnce()
        const [url, body, requestConfig] = http.post.mock.calls[0]
        expect(url).toMatch(/\/oauth2\/token$/)
        expect(body).toBeInstanceOf(URLSearchParams)
        expect(body.toString()).toBe("grant_type=password&username=rain&password=secret")
        expect(requestConfig).toMatchObject({
            showErrorNotification: false,
            timeout: 3000,
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
                "X-Request-Source": "login"
            }
        })
    })

    it("loads user and menu data through the admin BFF route", async () => {
        http.get.mockResolvedValue({success: true})

        expect(auth.getUserInfo.url).toBe(`${config.API_URL}/admin/api/v1/users/me`)
        expect(auth.getRoutes.url).toBe(`${config.API_URL}/admin/api/v1/menus/routes`)

        await auth.getUserInfo.get()
        await auth.getRoutes.get()

        expect(http.get).toHaveBeenNthCalledWith(1, expect.stringMatching(/\/admin\/api\/v1\/users\/me$/))
        expect(http.get).toHaveBeenNthCalledWith(2, expect.stringMatching(/\/admin\/api\/v1\/menus\/routes$/))
    })
})
