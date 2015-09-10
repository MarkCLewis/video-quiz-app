/**
 * 
 */

function newSpecType(prefix) {
	var select = $("#funcNewSpecType");
	var type = select[0].selectedIndex;
	var list = $("#funcul");
	var specIndex = list.find("li").length;
	var idCode = type+'-'+specIndex;
	var specCode = "";
	if(type === 0) { // @VariableSpec.IntSpecType
		specCode = 
			'<strong>Int - </strong>'+
			'Name: <input name="isVName-'+idCode+' id="isVName-'+idCode+'" value="" placeholder="Variable name"/>'+
			'Min: <input name="isMin-'+idCode+'" id="isMin-'+idCode+'" value="" placeholder="min value"/>'+
			'Max: <input name="isMax-'+idCode+'" id="isMax-'+idCode+'" value="" placeholder="max value"/>';
	} else 	if(type === 1) { // @VariableSpec.DoubleSpecType
		specCode = 
			'<strong>Double - </strong>'+
			'Name: <input name="dsVName-'+idCode+' id="dsVName-'+idCode+'" value="" placeholder="Variable name"/>'+
			'Min: <input name="dsMin-'+idCode+'" id="dsMin-'+idCode+'" value="" placeholder="min value"/>'+
			'Max: <input name="dsMax-'+idCode+'" id="dsMax-'+idCode+'" value="" placeholder="max value"/>';
	}
	// TODO
	
	list.append('<li id="'+prefix+'il-'+specIndex+'">'+specCode+'<button type="button" onclick="removeSpec(\''+prefix+'\','+specIndex+')">Remove</button></li>');
}

function removeSpec(prefix, index) {
	$('#'+prefix+'il-'+index).remove();
}

function multipleChoiceFocusLost(index) {
	var ol = $("#mcol");
	var text = $("#opt-"+index);
	var length = ol.find("li").length
	if(index === length-1 && text.val().length !== 0 && length<8) {
		ol.append('<li id="mcil-'+length+'"><input type="text" name="opt-'+length+'" id="opt-'+length+'" value="" placeholder="Option" onfocusout="multipleChoiceFocusLost('+length+')"/></li>');
	}
}

function associateMCWithQuiz(mcid) {
	var quizSelect = $("#quiz-mc-"+mcid);
	var quizid = quizSelect.val();
	var request = $.ajax({
		method: "PUT",
		url: "addMCToQuiz?questionid="+mcid+"&quizid="+quizid
	});
	request.done(function () {
		alert("Association made.");
	});
	request.fail(function( jqXHR, textStatus ) {
		alert( "Request failed: " + textStatus );
	});
}

function associateExprWithQuiz(id) {
	var quizSelect = $("#quiz-expr-"+id);
	var quizid = quizSelect.val();
	var request = $.ajax({
		method: "PUT",
		url: "addExprToQuiz?questionid="+id+"&quizid="+quizid
	});
	request.done(function () {
		alert("Association made.");
	});
	request.fail(function( jqXHR, textStatus ) {
		alert( "Request failed: " + textStatus );
	});
}

function removeQuestionQuizAssoc(questionid, questionType, quizid,index) {
	$("#prob-"+index).remove();
	var request = $.ajax({
		method: "DELETE",
		url: "removeQuestionFromQuiz?questionid="+questionid+"&questionType="+questionType+"&quizid="+quizid
	});
	request.done(function () {
		alert("Association removed.");
	});
	request.fail(function( jqXHR, textStatus ) {
		alert( "Request failed: " + textStatus );
	});
}

function associateQuizWithCourse(quizid) {
	var courseSelect = $("#qr-sel-"+quizid);
	var courseid = courseSelect.val();
	var dateTime = $("#qr-datetime-"+quizid).val();
	var request = $.ajax({
		method: "PUT",
		url: "addQuizToCourse?quizid="+quizid+"&courseid="+courseid+"&dateTime="+dateTime
	});
	request.done(function () {
		alert("Association made.");
	});
	request.fail(function( jqXHR, textStatus ) {
		alert( "Request failed: " + textStatus );
	});
}

function createUser() {
	var username = $("#newUserName").val();
	var userid = $("#newUserId").val();
	var request = $.ajax({
		method: "PUT",
		url: "createUser?username="+username+"&id="+userid
	});
	request.done(function () {
		alert("User made.");
	});
	request.fail(function( jqXHR, textStatus ) {
		alert( "Request failed: " + textStatus );
	});
}