<?php
include "vendor/autoload.php";
$m_server   = 'broker.hivemq.com';
$m_port     = 1883;
$m_clientId = 'smsgateway_'.rand(100000,999999);
$m_user = '';
$m_pass = '';

$to = urldecode($_REQUEST['to']);
$message = urldecode($_REQUEST['message']);
$deviceID = urldecode($_REQUEST['deviceID']);
$sim = urldecode($_REQUEST['sim'])*1;

if(empty($to) || empty($message) || empty($deviceID)){
    die("?to=&message=&deviceID=&sim=");
}

$fields = json_encode (
    array (
        "to" => $to,
        "message" => $message,
        "sim" => $sim
    )
);

try{
    $mqtt = new \PhpMqtt\Client\MqttClient($m_server, $m_port, $m_clientId);
    if(!empty($m_user) && !empty($m_pass)){
        $connectionSettings = (new \PhpMqtt\Client\ConnectionSettings)
        ->setUsername($m_user)
        ->setPassword($m_pass);
        $mqtt->connect($connectionSettings);
    }else{
        $mqtt->connect();
    }
    $mqtt->publish(
        $deviceID,
        $fields,
        0
    );
    $mqtt->disconnect();
    echo "success";
}catch (\PhpMqtt\Client\Exceptions\MqttClientException $e) {
    echo "failed";
}
