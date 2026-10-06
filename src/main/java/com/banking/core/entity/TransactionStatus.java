package com.banking.core.entity;

/**
 * Para transferinin olası durumları.
 */
public enum TransactionStatus {
    /** Transfer başlatıldı, işlem devam ediyor. */
    PENDING,

    /** Transfer başarıyla tamamlandı (commit edildi). */
    COMPLETED,

    /** İş kuralı ihlali nedeniyle transfer reddedildi (yetersiz bakiye vb.). */
    FAILED,

    /** Sistem hatası nedeniyle işlem geri alındı (rollback). */
    ROLLED_BACK
}
