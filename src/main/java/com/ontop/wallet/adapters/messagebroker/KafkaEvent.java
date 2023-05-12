package com.ontop.wallet.adapters.messagebroker;

import lombok.NonNull;

public record KafkaEvent(@NonNull Long transferId) { }
