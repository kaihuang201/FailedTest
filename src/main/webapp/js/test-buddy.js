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

clearFilters = function(tableID) {
	jQuery('#'+tableID+'Filters').find('input[type="text"],select').each(function() {
		jQuery(this).val('');
	});
	
	jQuery('#'+tableID).find('tbody').find('tr:hidden').each(function() {
		jQuery(this).show();
	});
}

filterBuilds = function() {
	var positiveIntegerRegex = /^[0-9]+?$/;
	var positiveFloatRegex = /(^[0-9]+?(\.[0-9]*?)?$)|(^\.[0-9]+?$)/;

	var buildStatus = jQuery('#buildStatus').val();
	var author = jQuery('#author').val();
	
	var minPassingRate = jQuery('#minPassingRate').val();
	if (!positiveIntegerRegex.test(minPassingRate)) {
		minPassingRate = '';
	}
	if (minPassingRate != '') {
		minPassingRate = parseFloat(minPassingRate);
	}

	var minPassedTests = jQuery('#minPassedTests').val();
	if (!positiveFloatRegex.test(minPassedTests)) {
		minPassedTests = '';
	}
	if (minPassedTests != '') {
		minPassedTests = parseInt(minPassedTests);
	}
	
	jQuery('#tblBuilds').find('tbody').find('tr').each(function() {
		if (buildStatus != '') {
			var status = jQuery(this).find('td[data-col="status"]').text();
			if (status != buildStatus) {
				jQuery(this).hide();
				return;
			}
		}

		if (author != '') {
			var authors = jQuery(this).find('td[data-col="authors"]').text().split(", ");
			if (jQuery.inArray(author, authors) == -1) {
				jQuery(this).hide();
				return;
			}
		}
		
		if (minPassingRate != '') {
			var passingRate = parseFloat(jQuery(this).find('td[data-col="passingRate"]').text());
			if (passingRate < minPassingRate) {
				jQuery(this).hide();
				return;
			}
		}

		if (minPassedTests != '') {
			var passedTests = parseInt(jQuery(this).find('td[data-col="passedTests"]').text());
			if (passedTests < minPassedTests) {
				jQuery(this).hide();
				return;
			}
		}
		
		jQuery(this).show();
	});
}