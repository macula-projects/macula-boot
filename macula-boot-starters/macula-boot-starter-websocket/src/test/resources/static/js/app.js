const stompClient = new StompJs.Client({
    brokerURL: 'ws://localhost:8083/websocket',
    connectHeaders: {
        name: "1234"
    },
    onConnect: function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        // DEMO
        stompClient.subscribe('/topic/test/greetings', (greeting) => {
            showGreeting(JSON.parse(greeting.body).content);
        });

        // 订阅群组消息
        stompClient.subscribe('/topic/test/group/' + groupId, function (greeting) {
            showGreeting(JSON.parse(greeting.body).content);
        });

        // 订阅私人消息
        stompClient.subscribe('/user/queue/test/me', function (greeting) {
            showGreeting(JSON.parse(greeting.body).content);
        });

        // 订阅他人发给我的消息
        stompClient.subscribe('/user/queue/test/chat', function (greeting) {
            showGreeting(JSON.parse(greeting.body).content);
        });
    },
    // If disconnected, it will retry after 200ms
    reconnectDelay: 10000,

    debug: function (str) {
        console.log(str);
    },

    onWebSocketError: function (error) {
        console.error('Error with websocket', error);
    },

    onStompError: function (frame) {
        console.error('Broker reported error: ' + frame.headers['message']);
        console.error('Additional details: ' + frame.body);
    }
});


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
        destination: "/app/test/hello",
        body: JSON.stringify({
            'name': $("#name").val()
        })
    });
}

function sendName2() {
    $.post("/test/hello2/" + groupId, {
        name: $("#name").val()
    });
}

function sendName3() {
    stompClient.publish({
        destination: "/app/test/me",
        body: JSON.stringify({
            'name': $("#name").val()
        })
    });
}

function sendName4() {
    stompClient.publish({
        destination: "/app/test/chat/" + $("#userId").val(),
        body: JSON.stringify({
            'name': $("#name").val()
        })
    });
}

function showGreeting(message) {
    $("#greetings").append("<tr><td>" + message + "</td></tr>");
}

$(function () {
    groupId = 123;
    $("form").on('submit', (e) => e.preventDefault());
    $("#connect").click(() => connect());
    $("#disconnect").click(() => disconnect());
    $("#send").click(() => sendName());
    $("#send2").click(() => sendName2());
    $("#send3").click(() => sendName3());
    $("#send4").click(() => sendName4());
});