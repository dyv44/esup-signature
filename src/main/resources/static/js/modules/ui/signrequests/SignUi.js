import {WorkspacePdf} from "./WorkspacePdf.js";
import {CsrfToken} from "../../../prototypes/CsrfToken.js";
import {PrintDocument} from "../../utils/PrintDocument.js";

export class SignUi {

    constructor(id, currentSignRequestParams, currentSignType, signWidth, signHeight, signable, postits, isPdf, currentStepNumber, signImages, userName , csrf) {
        console.info("Starting sign UI");
        this.signRequestId = id;
        this.percent = 0;
        this.getProgressTimer = null;
        this.wait = $('#wait');
        this.passwordError = document.getElementById("passwordError");
        this.workspace = null;
        this.signForm = document.getElementById("signForm");
        if(isPdf) {
            this.workspace = new WorkspacePdf(id, currentSignRequestParams, currentSignType, signWidth, signHeight, signable, postits, currentStepNumber, signImages, userName);
        }
        this.csrf = new CsrfToken(csrf);
        this.xmlHttpMain = new XMLHttpRequest();
        this.signRequestUrlParams = "";
        this.signComment = $('#signComment');
        this.signModal = $('#signModal');
        this.printDocument = new PrintDocument();
        this.initListeners();
    }

    initListeners() {
        $("#launchSignButton").on('click', e => this.launchSign());
        //$("#launchAllSignButton").on('click', e => this.launchAllSign());
        $("#password").on('keyup', function (e) {
            if (e.keyCode === 13) {
                $("#launchSignButton").click();
            }
        });
        $("#copyButton").on('click', e => this.copy());
        $("#print").on('click', e => this.launchPrint());
        document.addEventListener("sign", e => this.updateWaitModal(e));
    }

    launchPrint() {
        this.printDocument.launchPrint("/user/signrequests/get-last-file-base-64/" + this.signRequestId)
    }

    launchSign() {
        this.signModal.modal('hide');
        this.percent = 0;
        let inputs = this.signForm.getElementsByTagName("input");
        let good = true;
        for(var i = 0, len = inputs.length; i < len; i++) {
            let input = inputs[i];
            if(!input.checkValidity()) {
                good = false;
            }
        }
        if(good) {
            console.log('launch sign for : ' + this.signRequestId);
            this.wait.modal('show');
            this.wait.modal({backdrop: 'static', keyboard: false});
            this.submitSignRequest();
        } else {
            this.signModal.on('hidden.bs.modal', function () {
                $("#checkDataSubmit").click();
            })
        }
    }

    // launchAllSign() {
    //     $('#signAllModal').modal('hide');
    //     this.wait.modal('show');
    //     this.wait.modal({backdrop: 'static', keyboard: false});
    //     let csrf = document.getElementsByName("_csrf")[0];
    //     let signRequestParams = "password=" + document.getElementById("passwordAll").value +
    //         "&" + csrf.name + "=" + csrf.value;
    //     let xmlHttp = new XMLHttpRequest();
    //     xmlHttp.open('POST', '/user/signbooks/sign/' + this.signRequestId, true);
    //     xmlHttp.setRequestHeader('Content-Type','application/x-www-form-urlencoded');
    //     xmlHttp.send(signRequestParams);
    // }

    submitSignRequest() {
        let formData = { };
        $.each($('#signForm').serializeArray(), function() {
            if(!this.name.startsWith("comment")) {
                formData[this.name] = this.value;
            }
        });
        if(this.workspace != null) {
            this.signRequestUrlParams = "password=" + document.getElementById("password").value +
                "&signRequestParams=" + JSON.stringify(this.workspace.signPosition.signRequestParamses) +
                "&visual=" + this.workspace.signPosition.visualActive +
                "&comment=" + this.signComment.val() +
                "&formData=" + JSON.stringify(formData) +
                "&" + this.csrf.parameterName + "=" + this.csrf.token
            ;
        } else {
            this.signRequestUrlParams = "password=" + document.getElementById("password").value +
                "&" + this.csrf.name + "=" + this.csrf.value;
        }
        console.info("params to send : " + this.signRequestUrlParams);
        this.sendData(this.signRequestUrlParams);
    }

    sendData(signRequestUrlParams) {
        this.reset();
        this.xmlHttpMain.open('POST', '/user/signrequests/sign/' + this.signRequestId, true);
        //this.xmlHttpMain.addEventListener('readystatechange', e => this.end());
        this.xmlHttpMain.setRequestHeader('Content-Type','application/x-www-form-urlencoded');
        // this.xmlHttpMain.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
        this.xmlHttpMain.send(signRequestUrlParams);
        //this.getProgressTimer = setInterval(e => this.getStep(), 500);
    }

    updateWaitModal(e) {
        console.info("update wait modal");
        let message = e.detail
        console.info(message);
        this.percent = this.percent + 5;
        if (message.type === "security_bad_password") {
            console.error("sign error : bad password");
            this.passwordError.style.display = "block";
            document.getElementById("closeModal").style.display = "block";
            document.getElementById("bar").classList.remove("progress-bar-animated");
        } else if(message.type === "sign_system_error" || message.type === "not_authorized") {
            console.error("sign error : system error");
            document.getElementById("signError").style.display = "block";
            document.getElementById("closeModal").style.display = "block";
            document.getElementById("bar").classList.remove("progress-bar-animated");
            this.reset();
        } else if(message.type === "initNexu") {
            console.info("redirect to NexU sign proccess");
            document.location.href="/user/nexu-sign/" + this.signRequestId + "?" + this.signRequestUrlParams;
        }else if(message.type === "end") {
            console.info("sign end");
            document.getElementById("bar-text").innerHTML = "";
            document.getElementById("bar").classList.remove("progress-bar-animated");
            document.getElementById("bar-text").innerHTML = message.text;
            document.getElementById("bar").style.width = 100 + "%";
            document.location.href="/user/signrequests/" + this.signRequestId;
        } else {
            console.debug("update bar");
            document.getElementById("bar").style.display = "block";
            document.getElementById("bar").style.width = this.percent + "%";
            document.getElementById("bar-text").innerHTML = message.text;
        }
    }


    getStep() {
        this.percent = this.percent + 2;
        let xmlHttp = new XMLHttpRequest();
        xmlHttp.open("GET", "/user/signrequests/get-step", true);
        xmlHttp.addEventListener('readystatechange', e => this.updateWaitModal(xmlHttp));
        xmlHttp.send(null);
    }

    reset() {
        this.percent = 0;
        this.passwordError.style.display = "none";
        document.getElementById("signError").style.display = "none";
        document.getElementById("closeModal").style.display = "none";
        document.getElementById("validModal").style.display = "none";
        document.getElementById("bar").style.display = "none";
        document.getElementById("bar").classList.add("progress-bar-animated");
    }

    // updateWaitModal(xmlHttp) {
    //     let result = xmlHttp.responseText;
    //     console.log("getStep : " + result);
    //     if (result === "security_bad_password") {
    //         console.error("bad password");
    //         this.passwordError.style.display = "block";
    //         clearInterval(this.getProgressTimer);
    //         document.getElementById("closeModal").style.display = "block";
    //         document.getElementById("bar").classList.remove("progress-bar-animated");
    //     } else if(result === "sign_system_error" || result === "not_authorized") {
    //         console.error("bad password");
    //         clearInterval(this.getProgressTimer);
    //         document.getElementById("signError").style.display = "block";
    //         document.getElementById("closeModal").style.display = "block";
    //         document.getElementById("bar").classList.remove("progress-bar-animated");
    //         this.reset();
    //     } else if(result === "initNexu") {
    //         console.info("redirect to NexU sign proccess");
    //         clearInterval(this.getProgressTimer);
    //         document.location.href="/user/nexu-sign/" + this.signRequestId + "?" + this.signRequestUrlParams;
    //     }else if(result === "end") {
    //         console.info("get end");
    //         clearInterval(this.getProgressTimer);
    //         document.getElementById("bar-text").innerHTML = "";
    //         document.getElementById("bar").classList.remove("progress-bar-animated");
    //         document.getElementById("bar-text").innerHTML = "Signature terminée";
    //         document.getElementById("bar").style.width = 100 + "%";
    //         document.location.href="/user/signrequests/" + this.signRequestId;
    //     } else if(result !== ""){
    //         console.debug("update bar : " + result);
    //         document.getElementById("bar").style.display = "block";
    //         document.getElementById("bar").style.width = this.percent + "%";
    //         document.getElementById("bar-text").innerHTML = result;
    //     }
    // }

    end() {
        if(this.xmlHttpMain.status === 200) {
            console.info("sign end");
            document.getElementById("validModal").style.display = "block";
            setTimeout(e => this.redirect(),500);
        }
    }

    redirect() {
        document.location.href="/user/signrequests/" + this.signRequestId;
    }

    copy() {
        let copyText = document.getElementById("exportUrl");
        let textArea = document.createElement("textarea");
        textArea.value = copyText.textContent;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand("Copy");
        textArea.remove();
    }
}