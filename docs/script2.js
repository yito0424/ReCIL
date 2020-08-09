let localStream = null;
let peer = null; 
let existingCall = null;

// get Camera and Audio 
navigator.mediaDevices.getUserMedia({ video: true, audio: true })
    .then(function (stream) {
        // Success
        //$('#my-video').get(0).srcObject = stream;
        localStream = stream;
    }).catch(function (error) {
        // Error
        console.error('mediaDevice.getUserMedia() error:', error);
        return;
    })

// Event on Peer ID
peer = new Peer({
    key: '1b21eb31-978a-487d-9f1a-e117aa3a26a7',
    //key: '3c2ced1a-23f2-42df-b854-aca9cd9a8e7f',
    //key: '42b8cfa7-abb3-4762-9944-fb4b505daadc',
    debug: 3
});

peer.on('open', function () {
    $('#my-id').text(peer.id);
});
peer.on('error', function (err) {
    alert(err.message);
});
peer.on('close', function () {
});
peer.on('disconnected', function () {
});

//Event of Call
$('#make-call').submit(function (e) {
    e.preventDefault();
    const call = peer.call($('#callto-id').val(), localStream);
    setupCallEventHandlers(call);
});

//Event of Close 
$('#end-call').click(function () {
    existingCall.close();
});

//Event of Cathc
peer.on('call', function (call) {
    call.answer(localStream);
    setupCallEventHandlers(call);
});

//exiting Call (Assume that someone call me when I call other person.)
//Priority is after calling one
function setupCallEventHandlers(call) {
    if (existingCall) {
        existingCall.close();
    };

    existingCall = call;
    call.on('stream', function (stream) {
        addVideo(call, stream);
        setupEndCallUI();
        $('#their-id').text($('#callto-id').val());
        document.getElementById("make-call").style.visibility='hidden';
        document.getElementById("end-call").style.visibility='visible';
	
   	 });
    call.on('close', function () {
        removeVideo(call.remoteID);
        document.getElementById("make-call").style.visibility='visible';
        document.getElementById("end-call").style.visibility='hidden';
        setupMakeCallUI();
    });
}

//video opne
function addVideo(call, stream) {
    document.getElementById("their-video").style.visibility='visible';
    $('#their-video').get(0).srcObject = stream;
}
//vido close
function removeVideo(peerId) {
    document.getElementById("their-video").style.visibility='hidden';
    $('#their-video').get(0).srcObject = undefined;
}

//botton UI
function setupMakeCallUI() {
    $('#make-call').show();
    $('#end-call').hide();
}

function setupEndCallUI() {
    $('#make-call').hide();
    $('#end-call').show();
}


// Keyboad Interrupt
var sendflag = false;

window.addEventListener("keydown", handlekeydown);
function handlekeydown(event) {
    var keyCode = event.keyCode;
    console.log(keyCode);

    if (keyCode == 32) {
        sendflag = !sendflag;
        console.log('CHANGE THE MODE!');
        if(sendfalg==true){
            
        }
    }
}

// Mouse Event
var mX;
var mY;

var max_sentence=10;

var elem= document.getElementById("their-video");

elem.addEventListener("click",function(e){
    var i=2;
    var x=e.pageX;
    var y=e.pageY;
    var splitX = new Array(3);
    var splitY = new Array(3);
    
    var rect=this.getBoundingClientRect();
    mX=String(Math.round((x-rect.left+1)*255/(rect.right-rect.left+1)));
    mY=String(Math.round((y-rect.top+1)*255/(rect.bottom-rect.top+1)));

    for (let step = mX.length; step >0; step--) {
        splitX[i]=mX.charAt(step-1);
        i--;
    }
    i=2;
    for (let step = mY.length; step >0; step--) {
        splitY[i]=mY.charAt(step-1);
        i--;
    }
    for(let step = 0; step <3; step++){
        if(splitX[step]==null){splitX[step]=0};
        if(splitY[step]==null){splitY[step]=0};
    }
    console.log('X座標'+splitX+'Y座標'+splitY+'幅'+(rect.right-rect.left)+'高さ'+(rect.bottom-rect.top));
    
    const dataConnection=peer.connect($('#callto-id').val());
    dataConnection.on('open', () => {
            const data = {
                            name: 'Information',
                            msgX: splitX,
                            msgY: splitY
                            };
            dataConnection.send(data);
            console.log(data.name+":"+typeof data.msgX + ":"+data.msgY);
    });
    dataConnection.close();
});


//queueing per 1000 ms
/*
var count=0;
function stacking(){

	queueX.push(mX);
    queueY.push(mY);
    count++;
    console.log(queueY);

}
*/
/*
function promiseQueue(){
    return new Promise(function(resolve){
        console.log("seted");
        var id =setInterval(function(){
            //stacking();
            queueX.push(mX);
            queueY.push(mY);
            count++;
            console.log(queueX);
            if(count>max_sentence){

                //clearInterval(id);

            }},1000);

        count=0;
        resolve();
        

    });
}

//sending 
function promiseSend (){
    return new Promise(function(resolve,reject){
        const dataConnection=peer.connect($('#callto-id').val());
        dataConnection.on('open', () => {
                const data = {
                                name: 'Information',
                                msgX: queueX,
                                msgY: queueY
                                };
                dataConnection.send(data);
                console.log(data.name+":"+data.msgX + ":"+data.msgY);
                queueX.splice(0);
                queueY.splice(0);
        });
        resolve();        
    });
}

//delete Queue
/*
function promiseDelete(){
    return new Promise(function(resolve){
        //queueX.splice(0);
        //queueY.splice(0);
        console.log("delete");
        resolve();
    });
}*/


/*

var myPromise=Promise.resolve();

var sendd= async function sending(){
    if(sendflag==true){
        //await promiseQueue();
        //await promiseSend();
    
    }
}

setInterval(sendd,1000);

// Recieve data
peer.on('connection', dataConnection => {
    dataConnection.on('data', data => {
        console.log(data.msg);
    });
});

//document.onkeydown = function(e) {
//    var keyCode = false;

//    if (e) event = e;

//    if (event) {
//        if (event.keyCode) {
//            keyCode = event.keyCode;
//        } else if (event.which) {
//            keyCode = event.which;
//        }
//    }

//    console.log(keyCode);
//};


// Sending DATA 


*/
