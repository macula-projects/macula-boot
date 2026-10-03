/*
 * Copyright (c) 2023 Macula
 *   macula.dev, China
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// https://docs.cypress.io/api/introduction/api.html

describe("Admin login", () => {
    it("redirects the app root to the login page", () => {
        cy.visit("/");
        cy.location("hash").should("eq", "#/login");
        cy.contains("账号登录").should("be.visible");
        cy.contains("button", "登录").should("be.visible");
    });

    it("shows the IAM response message when password authentication fails", () => {
        cy.intercept("POST", "**/oauth2/token", {
            statusCode: 400,
            body: {
                error_description: "Bad credentials",
                error: "bad_credentials",
            },
        }).as("tokenRequest");

        cy.visit("/#/login");
        cy.contains("button", "登录").click();

        cy.wait("@tokenRequest");
        cy.contains(".el-message", "Bad credentials").should("be.visible");
        cy.contains("button", "登录").should("not.be.disabled");
    });

    it("clears partial authentication when user loading fails", () => {
        cy.intercept("POST", "**/oauth2/token", {
            statusCode: 200,
            body: {access_token: "temporary-token"},
        }).as("tokenRequest");
        cy.intercept("GET", "**/api/admin/api/v1/users/me*", {
            statusCode: 503,
            body: {message: "System unavailable"},
        }).as("userRequest");

        cy.visit("/#/login");
        cy.contains("button", "登录").click();

        cy.wait("@tokenRequest");
        cy.wait("@userRequest");
        cy.contains(".el-message", "System unavailable").should("be.visible");
        cy.contains("button", "登录").should("not.be.disabled");
        cy.getCookie("TOKEN").should("not.exist");
    });

    it("clears partial authentication when menu loading fails", () => {
        cy.intercept("POST", "**/oauth2/token", {
            statusCode: 200,
            body: {access_token: "temporary-token"},
        });
        cy.intercept("GET", "**/api/admin/api/v1/users/me*", {
            statusCode: 200,
            body: {success: true, data: {roles: [], perms: []}},
        });
        cy.intercept("GET", "**/api/admin/api/v1/menus/routes*", {
            statusCode: 503,
            body: {error_description: "Menu unavailable"},
        }).as("routesRequest");

        cy.visit("/#/login");
        cy.contains("button", "登录").click();

        cy.wait("@routesRequest");
        cy.contains(".el-message", "Menu unavailable").should("be.visible");
        cy.contains("button", "登录").should("not.be.disabled");
        cy.getCookie("TOKEN").should("not.exist");
    });

    it("keeps menus without role restrictions", () => {
        cy.intercept("POST", "**/oauth2/token", {
            statusCode: 200,
            body: {access_token: "valid-token"},
        });
        cy.intercept("GET", "**/api/admin/api/v1/users/me*", {
            statusCode: 200,
            body: {success: true, data: {roles: ["ADMIN"], perms: []}},
        });
        cy.intercept("GET", "**/api/admin/api/v1/menus/routes*", {
            statusCode: 200,
            body: {
                success: true,
                data: [{path: "/home", meta: {title: "首页", roles: []}}],
            },
        }).as("routesRequest");

        cy.visit("/#/login");
        cy.contains("button", "登录").click();

        cy.wait("@routesRequest");
        cy.window().then((win) => {
            expect(win.localStorage.getItem("MENU")).to.contain("首页");
        });
    });
});
