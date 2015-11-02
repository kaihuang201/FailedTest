function toggleReg() {
	if (document.getElementById('Regression').checked === false) {
		jQuery('#mytable tbody tr td:contains("Newly Failed")').parent().toggle();
	}
	else if (document.getElementById('Regression').checked === true){
		jQuery('#mytable tbody tr td:contains("Newly Failed")').parent().toggle();
	}
}
function toggleProg(){
	if (document.getElementById('Progression').checked === false) {
		jQuery('#mytable tbody tr td:contains("Newly Passed")').parent().toggle();	
	}
	else if (document.getElementById('Progression').checked === true) {
		jQuery('#mytable tbody tr td:contains("Newly Passed")').parent().toggle();
	}
}
