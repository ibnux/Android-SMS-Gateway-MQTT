<?php

$type = $_REQUEST['type'];
$number = $_REQUEST['number'];
$message = $_REQUEST['message'];

if($type == 'received'){
    /*
    $message is text received
    $number is phone who send message
    */
}else if($type == 'sent'){
    if($message != 'success'){
        // Failed to send
        // $message will be
        // - Generic failure
        // - No service
        // - Null PDU
        // - Radio off

    }else{
        // success sent to this $number
    }
}else if($type == 'delivered'){
    if($message == 'success'){
        // delivered
        // $number delivered to this number
    }else if($message == 'failed'){
        // undelivered
        // $number undelivered to this number
    }
}else if($type == 'ussd'){
    /*
    $message USSD Result
    $number USSD number
    */
}
