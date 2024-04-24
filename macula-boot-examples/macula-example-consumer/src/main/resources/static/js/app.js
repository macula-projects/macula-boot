const stompClient = new StompJs.Client({
    onConnect: function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);

        // DEMO
        stompClient.subscribe('/topic/greetings', (greeting) => {
            showGreeting(JSON.parse(greeting.body).content);
        });

        // 订阅群组消息
        stompClient.subscribe('/topic/group/' + groupId, function (greeting) {
            showGreeting(JSON.parse(greeting.body).content);
        });

        // 订阅私人消息
        stompClient.subscribe('/user/queue/me', function (greeting) {
            showGreeting(JSON.parse(greeting.body).content);
        });

        // 订阅他人发给我的消息
        stompClient.subscribe('/user/queue/chat', function (greeting) {
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
        disconnect();
    },
    onDisconnect: function () {
        disconnect();
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
    let token = $("#token").val()
    if (token !== undefined && token.length > 0) {
        stompClient.brokerURL = 'ws://localhost:8000/websocket/websocket?access_token=' + token
        stompClient.activate();
    } else {
        alert("Please input access token!")
    }
}

function disconnect() {
    stompClient.deactivate();
    setConnected(false);
    console.log("Disconnected");
}

function sendName() {
    stompClient.publish({
        destination: "/app/hello",
        headers: {
            'grayversion': "feat-123"
        },
        body: JSON.stringify({
            'name': $("#name").val()
        })
    });
}

function sendName2() {
    $.post("http://localhost:8000/consumer/hello2/" + groupId, {
        name: $("#name").val()
    });
}

function sendName3() {
    stompClient.publish({
        destination: "/app/me",
        body: JSON.stringify({
            'name': $("#name").val()
        })
    });
}

function sendName4() {
    stompClient.publish({
        destination: "/app/chat/" + $("#sendTo").val(),
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