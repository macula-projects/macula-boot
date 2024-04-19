const stompClient = new StompJs.Client({
    brokerURL: 'ws://localhost:8083/websocket'
});

stompClient.onConnect = (frame) => {
    setConnected(true);
    console.log('Connected: ' + frame);
    // DEMO
    stompClient.subscribe('/topic/greetings', (greeting) => {
        showGreeting(JSON.parse(greeting.body).content);
    });

    // 订阅群组消息
    stompClient.subscribe('/topic/topic/greetings', function (greeting) {
        showGreeting(JSON.parse(greeting.body).content);
    });

    // 订阅私人消息
    stompClient.subscribe('/topic/user/' + userId + '/sendToUser', function (greeting) {
        showGreeting(JSON.parse(greeting.body).content);
    });

    stompClient.subscribe('/user/' + userId + '/sendToUser', function (greeting) {
        showGreeting(JSON.parse(greeting.body).content);
    });
};

stompClient.debug = function (str) {
    console.log(str);
};

stompClient.onWebSocketError = (error) => {
    console.error('Error with websocket', error);
};

stompClient.onStompError = (frame) => {
    console.error('Broker reported error: ' + frame.headers['message']);
    console.error('Additional details: ' + frame.body);
};

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    if (connected) {
        $("#conversation").show();
    } else {
        $("#conversation").hide();
    }
    $("#greetings").html("");
}

function connect() {
    stompClient.activate();
}

function disconnect() {
    stompClient.deactivate();
    setConnected(false);
    console.log("Disconnected");
}

function sendName() {
    stompClient.publish({
        destination: "/app/hello",
        body: JSON.stringify({
            'name': $("#name").val()
        })
    });
}

function sendName2() {
    stompClient.publish({
        destination: "/app/topic/greetings",
        body: JSON.stringify({
            'name': $("#name").val()
        })
    });
}

function sendName3() {
    stompClient.publish({
        destination: "/app/sendToUser",
        body: JSON.stringify({
            'userId': $("#userId").val(),
            'name': $("#name").val()
        })
    });
}

function showGreeting(message) {
    $("#greetings").append("<tr><td>" + message + "</td></tr>");
}

function GetQueryString(name) {
    let reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
    let r = window.location.search.substring(1).match(reg); //获取url中"?"符后的字符串并正则匹配
    let context = "";
    if (r != null)
        context = r[2];
    reg = null;
    r = null;
    return context == null || context === "" || context === "undefined" ? "" : context;
}

$(function () {
    userId = GetQueryString("userId");
    $("form").on('submit', (e) => e.preventDefault());
    $("#connect").click(() => connect());
    $("#disconnect").click(() => disconnect());
    $("#send").click(() => sendName());
    $("#send2").click(() => sendName2());
    $("#send3").click(() => sendName3());
});